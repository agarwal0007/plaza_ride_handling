package com.ridehailing.service;

import com.ridehailing.discount.PercentageDiscount;
import com.ridehailing.exception.DomainException;
import com.ridehailing.matching.NearestDriverStrategy;
import com.ridehailing.model.CarType;
import com.ridehailing.model.DriverStatus;
import com.ridehailing.model.FareBreakdown;
import com.ridehailing.model.Location;
import com.ridehailing.model.Ride;
import com.ridehailing.model.RideStatus;
import com.ridehailing.pricing.PricingEngine;
import com.ridehailing.pricing.RateCardProvider;
import com.ridehailing.repo.CouponRepository;
import com.ridehailing.repo.DriverRepository;
import com.ridehailing.repo.InMemoryCouponRepository;
import com.ridehailing.repo.InMemoryDriverRepository;
import com.ridehailing.repo.InMemoryRideRepository;
import com.ridehailing.repo.InMemoryUserRepository;
import com.ridehailing.repo.RideRepository;
import com.ridehailing.repo.UserRepository;
import com.ridehailing.surge.NoSurge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RideServiceTest {

    private static final double RADIUS_KM = 5.0;

    private DriverRepository driverRepository;
    private DriverService driverService;
    private CouponService couponService;
    private RideService rideService;

    @BeforeEach
    void setUp() {
        UserRepository userRepository = new InMemoryUserRepository();
        driverRepository = new InMemoryDriverRepository();
        RideRepository rideRepository = new InMemoryRideRepository();
        CouponRepository couponRepository = new InMemoryCouponRepository();

        UserService userService = new UserService(userRepository);
        driverService = new DriverService(driverRepository);
        couponService = new CouponService(couponRepository);

        PricingEngine pricingEngine = new PricingEngine(new RateCardProvider());
        NearestDriverStrategy matchingStrategy = new NearestDriverStrategy();

        rideService = new RideService(userRepository, driverRepository, rideRepository,
                couponService, pricingEngine, matchingStrategy, new NoSurge(), RADIUS_KM);

        userService.registerUser("u1", "Alice");
    }

    @Test
    void bookingFailsWhenNoDriverIsWithinRadius() {
        driverService.registerDriver("d1", "Bob", CarType.SEDAN, "KA01", 100, 100);

        assertThrows(DomainException.class, () ->
                rideService.book("r1", "u1", CarType.SEDAN, new Location(0, 0), new Location(1, 1), null));
    }

    @Test
    void driverOutsideRadiusIsIgnored() {
        driverService.registerDriver("far", "Far", CarType.SEDAN, "KA01", 10, 10); // ~14.14km away, outside radius
        driverService.registerDriver("near", "Near", CarType.SEDAN, "KA02", 1, 0); // 1km away, within radius

        Ride ride = rideService.book("r1", "u1", CarType.SEDAN, new Location(0, 0), new Location(2, 2), null);

        assertEquals("near", ride.getDriverId());
    }

    @Test
    void hatchbackFallsBackToSedanAndIsStillPricedAsHatchback() {
        driverService.registerDriver("d1", "Bob", CarType.SEDAN, "KA01", 1, 0);

        Ride ride = rideService.book("r1", "u1", CarType.HATCHBACK, new Location(0, 0), new Location(7, 0), null);

        assertEquals(CarType.HATCHBACK, ride.getRequestedCarType());
        assertEquals(CarType.SEDAN, ride.getAssignedCarType());

        FareBreakdown fareBreakdown = rideService.endRide("r1");
        // 7km HATCHBACK fare, per the spec's worked example: 20 + 24 + 10 = 54.
        assertEquals(5400, fareBreakdown.total());
        assertEquals(CarType.HATCHBACK, fareBreakdown.pricedAsCarType());
    }

    @Test
    void invalidCouponFailsBookingAndLeavesDriverAvailable() {
        driverService.registerDriver("d1", "Bob", CarType.SEDAN, "KA01", 1, 0);

        assertThrows(DomainException.class, () ->
                rideService.book("r1", "u1", CarType.SEDAN, new Location(0, 0), new Location(1, 1), "NOPE"));

        assertTrue(rideService.getUserHistory("u1").isEmpty());
        assertEquals(DriverStatus.AVAILABLE, driverRepository.findById("d1").orElseThrow().getStatus());
    }

    @Test
    void couponDeletedMidRideStillAppliesAtEnd() {
        driverService.registerDriver("d1", "Bob", CarType.HATCHBACK, "KA01", 0, 0);
        couponService.addCoupon("SAVE10", new PercentageDiscount(10, 100_000));

        rideService.book("r1", "u1", CarType.HATCHBACK, new Location(0, 0), new Location(7, 0), "SAVE10");
        couponService.deleteCoupon("SAVE10");

        FareBreakdown fareBreakdown = rideService.endRide("r1");
        // 10% off the 5400 fare, even though the coupon no longer exists.
        assertEquals(540, fareBreakdown.discount());
        assertEquals(4860, fareBreakdown.total());
    }

    @Test
    void historyShowsOngoingThenCompletedForBothUserAndDriver() {
        driverService.registerDriver("d1", "Bob", CarType.SEDAN, "KA01", 0, 0);
        rideService.book("r1", "u1", CarType.SEDAN, new Location(0, 0), new Location(1, 0), null);

        Map<RideStatus, List<Ride>> userHistoryBefore = rideService.getUserHistory("u1");
        assertEquals(1, userHistoryBefore.get(RideStatus.ONGOING).size());
        assertNull(userHistoryBefore.get(RideStatus.COMPLETED));

        Map<RideStatus, List<Ride>> driverHistoryBefore = rideService.getDriverHistory("d1");
        assertEquals(1, driverHistoryBefore.get(RideStatus.ONGOING).size());

        rideService.endRide("r1");

        Map<RideStatus, List<Ride>> userHistoryAfter = rideService.getUserHistory("u1");
        assertNull(userHistoryAfter.get(RideStatus.ONGOING));
        assertEquals(1, userHistoryAfter.get(RideStatus.COMPLETED).size());

        Map<RideStatus, List<Ride>> driverHistoryAfter = rideService.getDriverHistory("d1");
        assertEquals(1, driverHistoryAfter.get(RideStatus.COMPLETED).size());
    }

    @Test
    void doubleBookingForSameUserFails() {
        driverService.registerDriver("d1", "Bob", CarType.SEDAN, "KA01", 0, 0);
        driverService.registerDriver("d2", "Carl", CarType.SEDAN, "KA02", 0, 0);

        rideService.book("r1", "u1", CarType.SEDAN, new Location(0, 0), new Location(1, 0), null);

        assertThrows(DomainException.class, () ->
                rideService.book("r2", "u1", CarType.SEDAN, new Location(0, 0), new Location(1, 0), null));
    }

    @Test
    void endingARideTwiceFails() {
        driverService.registerDriver("d1", "Bob", CarType.SEDAN, "KA01", 0, 0);
        rideService.book("r1", "u1", CarType.SEDAN, new Location(0, 0), new Location(1, 0), null);

        rideService.endRide("r1");

        assertThrows(DomainException.class, () -> rideService.endRide("r1"));
    }

    @Test
    void driverLocationMovesToDropPointAfterRideEnds() {
        driverService.registerDriver("d1", "Bob", CarType.SEDAN, "KA01", 0, 0);
        rideService.book("r1", "u1", CarType.SEDAN, new Location(0, 0), new Location(3, 4), null);

        rideService.endRide("r1");

        Location newLocation = driverRepository.findById("d1").orElseThrow().getCab().getLocation();
        assertEquals(3, newLocation.getX());
        assertEquals(4, newLocation.getY());
    }
}
