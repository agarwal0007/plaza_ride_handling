package com.ridehailing.service;

import com.ridehailing.cancellation.TimeBasedCancellationPolicy;
import com.ridehailing.exception.DomainException;
import com.ridehailing.matching.NearestDriverStrategy;
import com.ridehailing.model.CarType;
import com.ridehailing.model.Location;
import com.ridehailing.pricing.PricingEngine;
import com.ridehailing.pricing.RateCardProvider;
import com.ridehailing.repo.InMemoryCouponRepository;
import com.ridehailing.repo.InMemoryDriverRepository;
import com.ridehailing.repo.InMemoryRideRepository;
import com.ridehailing.repo.InMemoryUserRepository;
import com.ridehailing.surge.NoSurge;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Separated from RideServiceTest because this one depends on real thread timing
 * rather than pure call/response behaviour (decision #11: claiming a driver must be
 * an atomic compare-and-swap, so two riders racing for the same driver can't both win).
 */
class RideServiceConcurrencyTest {

    @Test
    void onlyOneOfTwoConcurrentBookingsSucceedsForASingleAvailableDriver() throws InterruptedException {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        InMemoryDriverRepository driverRepository = new InMemoryDriverRepository();
        InMemoryRideRepository rideRepository = new InMemoryRideRepository();

        UserService userService = new UserService(userRepository);
        DriverService driverService = new DriverService(driverRepository);
        CouponService couponService = new CouponService(new InMemoryCouponRepository());
        RideService rideService = new RideService(userRepository, driverRepository, rideRepository,
                couponService, new PricingEngine(new RateCardProvider()), new NearestDriverStrategy(),
                new NoSurge(), new TimeBasedCancellationPolicy(), Clock.systemUTC(), 5.0);

        userService.registerUser("u1", "Alice");
        userService.registerUser("u2", "Bob");
        driverService.registerDriver("d1", "Carl", CarType.SEDAN, "KA01", 0, 0);

        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        Runnable book1 = bookingAttempt(rideService, startLatch, successes, failures, "r1", "u1");
        Runnable book2 = bookingAttempt(rideService, startLatch, successes, failures, "r2", "u2");

        Thread t1 = new Thread(book1);
        Thread t2 = new Thread(book2);
        t1.start();
        t2.start();

        startLatch.countDown(); // release both threads at the same instant

        t1.join();
        t2.join();

        assertEquals(1, successes.get());
        assertEquals(1, failures.get());
    }

    private Runnable bookingAttempt(RideService rideService, CountDownLatch startLatch,
                                     AtomicInteger successes, AtomicInteger failures,
                                     String rideId, String userId) {
        return () -> {
            try {
                startLatch.await();
                rideService.book(rideId, userId, CarType.SEDAN, new Location(0, 0), new Location(1, 0), null);
                successes.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (DomainException e) {
                failures.incrementAndGet();
            }
        };
    }
}
