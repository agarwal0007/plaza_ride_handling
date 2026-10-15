package com.ridehailing.service;

import com.ridehailing.matching.NearestDriverStrategy;
import com.ridehailing.model.CarType;
import com.ridehailing.model.FareBreakdown;
import com.ridehailing.model.Location;
import com.ridehailing.model.Ride;
import com.ridehailing.pricing.PricingEngine;
import com.ridehailing.pricing.RateCardProvider;
import com.ridehailing.repo.DriverRepository;
import com.ridehailing.repo.InMemoryCouponRepository;
import com.ridehailing.repo.InMemoryDriverRepository;
import com.ridehailing.repo.InMemoryRideRepository;
import com.ridehailing.repo.InMemoryUserRepository;
import com.ridehailing.repo.RideRepository;
import com.ridehailing.repo.UserRepository;
import com.ridehailing.surge.DemandSupplySurge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Separated from RideServiceTest because these need DemandSupplySurge wired in (that
 * test class wires NoSurge, so its fare assertions stay exactly as they were before
 * surge pricing existed).
 */
class RideServiceSurgeTest {

    private static final double RADIUS_KM = 5.0;

    private UserService userService;
    private DriverService driverService;
    private RideService rideService;

    @BeforeEach
    void setUp() {
        UserRepository userRepository = new InMemoryUserRepository();
        DriverRepository driverRepository = new InMemoryDriverRepository();
        RideRepository rideRepository = new InMemoryRideRepository();

        userService = new UserService(userRepository);
        driverService = new DriverService(driverRepository);
        CouponService couponService = new CouponService(new InMemoryCouponRepository());
        PricingEngine pricingEngine = new PricingEngine(new RateCardProvider());
        DemandSupplySurge surgeStrategy = new DemandSupplySurge(driverRepository, rideRepository, RADIUS_KM);

        rideService = new RideService(userRepository, driverRepository, rideRepository, couponService,
                pricingEngine, new NearestDriverStrategy(), surgeStrategy, RADIUS_KM);

        userService.registerUser("u1", "Alice");
        userService.registerUser("u2", "Bob");
    }

    @Test
    void surgeIsLockedAtBookingAndUnaffectedByLaterDemandChanges() {
        // d1 is already mid-ride (r1, ongoing, pickup at (0,0)) when u2 books: at that
        // instant supply = 1 (just d2 - d1 is ON_TRIP) and demand = 1 (r1), so ratio =
        // 1 -> the 1.25x mid band. r2 locks that multiplier onto itself.
        driverService.registerDriver("d1", "Carl", CarType.SEDAN, "KA01", 0, 0);
        driverService.registerDriver("d2", "Dana", CarType.SEDAN, "KA02", 0, 0);
        rideService.book("r1", "u1", CarType.SEDAN, new Location(0, 0), new Location(1, 0), null);

        Ride r2 = rideService.book("r2", "u2", CarType.SEDAN, new Location(0, 0), new Location(7, 0), null);
        assertEquals(12_500, r2.getSurgeBps());

        // Now change demand: ending r1 removes it from the ongoing count, and frees d1
        // back up as supply too. If surge were recomputed at end time instead of locked
        // at booking, this would drop the ratio to 0 and r2's fare would come out lower.
        rideService.endRide("r1");

        FareBreakdown r2Fare = rideService.endRide("r2");
        // SEDAN 7km: tieredFare 6800 + minimumFareAdjustment 200 = 7000, then 1.25x.
        assertEquals(1750, r2Fare.surgeAdjustment());
        assertEquals(8750, r2Fare.total());
    }

    @Test
    void upgradedRideIsSurgedOnTheRequestedTypesFare() {
        // Two pre-existing ongoing SEDAN rides at this pickup make demand = 2. No
        // HATCHBACK driver exists at all, so supply (for a HATCHBACK request) is 0 ->
        // ratio = demand / max(0, 1) = 2 -> the 1.5x high band.
        driverService.registerDriver("busy1", "Eve", CarType.SEDAN, "KA03", 0, 0);
        driverService.registerDriver("busy2", "Frank", CarType.SEDAN, "KA04", 0, 0);
        rideService.book("busyRide1", "u1", CarType.SEDAN, new Location(0, 0), new Location(1, 0), null);
        rideService.book("busyRide2", "u2", CarType.SEDAN, new Location(0, 0), new Location(1, 0), null);

        // u1 and u2 both already have an ongoing ride, so this booking needs a third rider.
        userService.registerUser("u3", "Grace");
        driverService.registerDriver("d1", "Henry", CarType.SEDAN, "KA05", 0, 0);

        Ride ride = rideService.book("r1", "u3", CarType.HATCHBACK, new Location(0, 0), new Location(7, 0), null);
        assertEquals(CarType.HATCHBACK, ride.getRequestedCarType());
        assertEquals(CarType.SEDAN, ride.getAssignedCarType());
        assertEquals(15_000, ride.getSurgeBps());

        FareBreakdown fareBreakdown = rideService.endRide("r1");
        // HATCHBACK 7km: tieredFare 5400, no minimum-fare adjustment needed, then 1.5x.
        assertEquals(CarType.HATCHBACK, fareBreakdown.pricedAsCarType());
        assertEquals(2700, fareBreakdown.surgeAdjustment());
        assertEquals(8100, fareBreakdown.total());
    }
}
