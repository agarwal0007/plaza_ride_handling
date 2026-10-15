package com.ridehailing.service;

import com.ridehailing.cancellation.TimeBasedCancellationPolicy;
import com.ridehailing.exception.DomainException;
import com.ridehailing.matching.NearestDriverStrategy;
import com.ridehailing.model.CarType;
import com.ridehailing.model.DriverStatus;
import com.ridehailing.model.Location;
import com.ridehailing.model.Ride;
import com.ridehailing.model.RideStatus;
import com.ridehailing.pricing.PricingEngine;
import com.ridehailing.pricing.RateCardProvider;
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RideServiceCancellationTest {

    private static final double RADIUS_KM = 5.0;
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    /** A Clock whose instant() can be advanced on demand - java.time.Clock has no mutable implementation. */
    private static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void advanceBy(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private DriverRepository driverRepository;
    private RideService rideService;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        UserRepository userRepository = new InMemoryUserRepository();
        driverRepository = new InMemoryDriverRepository();
        RideRepository rideRepository = new InMemoryRideRepository();

        UserService userService = new UserService(userRepository);
        DriverService driverService = new DriverService(driverRepository);
        CouponService couponService = new CouponService(new InMemoryCouponRepository());
        PricingEngine pricingEngine = new PricingEngine(new RateCardProvider());
        clock = new MutableClock(T0, ZoneId.systemDefault());

        rideService = new RideService(userRepository, driverRepository, rideRepository, couponService,
                pricingEngine, new NearestDriverStrategy(), new NoSurge(),
                new TimeBasedCancellationPolicy(), clock, RADIUS_KM);

        userService.registerUser("u1", "Alice");
        driverService.registerDriver("d1", "Carl", CarType.SEDAN, "KA01", 0, 0);
    }

    private Ride bookRide() {
        return rideService.book("r1", "u1", CarType.SEDAN, new Location(0, 0), new Location(7, 0), null);
    }

    @Test
    void cancellingInsideTheGraceWindowIsFree() {
        bookRide();
        clock.advanceBy(Duration.ofMinutes(1));
        assertEquals(0, rideService.cancelRide("r1"));
    }

    @Test
    void cancellingAtExactlyTwoMinutesIsStillFree() {
        bookRide();
        clock.advanceBy(Duration.ofMinutes(2));
        assertEquals(0, rideService.cancelRide("r1"));
    }

    @Test
    void cancellingAfterTheGraceWindowChargesTheFlatFee() {
        bookRide();
        clock.advanceBy(Duration.ofMinutes(2).plusSeconds(1));
        assertEquals(2_500, rideService.cancelRide("r1"));
    }

    @Test
    void cancellingACompletedRideFails() {
        bookRide();
        rideService.endRide("r1");
        assertThrows(DomainException.class, () -> rideService.cancelRide("r1"));
    }

    @Test
    void cancellingAnAlreadyCancelledRideFails() {
        bookRide();
        rideService.cancelRide("r1");
        assertThrows(DomainException.class, () -> rideService.cancelRide("r1"));
    }

    @Test
    void driverIsBookableAgainAfterCancel() {
        bookRide();
        assertEquals(DriverStatus.ON_TRIP, driverRepository.findById("d1").orElseThrow().getStatus());

        rideService.cancelRide("r1");
        assertEquals(DriverStatus.AVAILABLE, driverRepository.findById("d1").orElseThrow().getStatus());

        // The ride never started, so the cab is still at its pre-ride location (0,0),
        // not the ride's drop point (7,0) - unlike what end_ride would have done.
        Location cabLocation = driverRepository.findById("d1").orElseThrow().getCab().getLocation();
        assertEquals(0.0, cabLocation.getX());
        assertEquals(0.0, cabLocation.getY());

        // "Bookable again" means an actual booking succeeds, not just the status flag.
        Ride secondRide = rideService.book("r2", "u1", CarType.SEDAN, new Location(0, 0), new Location(1, 0), null);
        assertEquals("d1", secondRide.getDriverId());
    }

    @Test
    void cancelledRidesAppearInTheCancelledHistoryGroupForUserAndDriver() {
        bookRide();
        rideService.cancelRide("r1");

        Map<RideStatus, List<Ride>> userHistory = rideService.getUserHistory("u1");
        assertEquals(1, userHistory.get(RideStatus.CANCELLED).size());
        assertEquals("r1", userHistory.get(RideStatus.CANCELLED).get(0).getRideId());

        Map<RideStatus, List<Ride>> driverHistory = rideService.getDriverHistory("d1");
        assertEquals(1, driverHistory.get(RideStatus.CANCELLED).size());
    }
}
