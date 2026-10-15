package com.ridehailing.service;

import com.ridehailing.exception.DomainException;
import com.ridehailing.matching.DriverMatchingStrategy;
import com.ridehailing.model.CarType;
import com.ridehailing.model.Coupon;
import com.ridehailing.model.Driver;
import com.ridehailing.model.FareBreakdown;
import com.ridehailing.model.Location;
import com.ridehailing.model.Ride;
import com.ridehailing.model.RideStatus;
import com.ridehailing.pricing.PricingEngine;
import com.ridehailing.repo.DriverRepository;
import com.ridehailing.repo.RideRepository;
import com.ridehailing.repo.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Books and ends rides. Deliberately has no pricing math and no driver-selection
 * logic of its own - it delegates those to PricingEngine and DriverMatchingStrategy,
 * both injected via the constructor. The one business rule it does own is the
 * HATCHBACK -> SEDAN fallback (decision #10: that's a pricing/business decision, not
 * a matching concern).
 */
public class RideService {
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final CouponService couponService;
    private final PricingEngine pricingEngine;
    private final DriverMatchingStrategy matchingStrategy;
    private final double radiusKm;

    public RideService(UserRepository userRepository,
                        DriverRepository driverRepository,
                        RideRepository rideRepository,
                        CouponService couponService,
                        PricingEngine pricingEngine,
                        DriverMatchingStrategy matchingStrategy,
                        double radiusKm) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
        this.couponService = couponService;
        this.pricingEngine = pricingEngine;
        this.matchingStrategy = matchingStrategy;
        this.radiusKm = radiusKm;
    }

    public Ride book(String rideId, String userId, CarType requestedCarType,
                      Location pickupLocation, Location dropLocation, String couponCode) {
        userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("No user registered with id: " + userId));

        boolean userHasOngoingRide = rideRepository.findByUserId(userId).stream()
                .anyMatch(ride -> ride.getStatus() == RideStatus.ONGOING);
        if (userHasOngoingRide) {
            throw new DomainException("User " + userId + " already has an ongoing ride.");
        }

        Driver assignedDriver = null;
        CarType assignedCarType = null;

        for (CarType carTypeToTry : carTypesToTry(requestedCarType)) {
            List<Driver> candidates = matchingStrategy.findCandidates(
                    carTypeToTry, pickupLocation, radiusKm, driverRepository.findAll());
            for (Driver candidate : candidates) {
                if (candidate.claim()) {
                    assignedDriver = candidate;
                    assignedCarType = carTypeToTry;
                    break;
                }
            }
            if (assignedDriver != null) {
                break;
            }
        }

        if (assignedDriver == null) {
            throw new DomainException(
                    "No available " + requestedCarType + " driver within " + radiusKm + " units.");
        }

        // The coupon is validated only after a driver is claimed, so an invalid coupon
        // must release that driver before failing - otherwise it would be stuck ON_TRIP
        // with no ride to show for it.
        Coupon coupon = null;
        if (couponCode != null && !couponCode.isBlank()) {
            try {
                coupon = couponService.validate(couponCode);
            } catch (DomainException e) {
                assignedDriver.release();
                throw e;
            }
        }

        Ride ride = new Ride(rideId, userId, assignedDriver.getDriverId(), requestedCarType,
                assignedCarType, pickupLocation, dropLocation);
        if (coupon != null) {
            ride.lockCoupon(coupon.getCode(), coupon.getDiscountStrategy());
        }
        rideRepository.save(ride);
        return ride;
    }

    public FareBreakdown endRide(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new DomainException("No ride found with id: " + rideId));

        double distanceKm = ride.getPickupLocation().distanceTo(ride.getDropLocation());
        FareBreakdown fareBreakdown = pricingEngine.price(
                distanceKm, ride.getRequestedCarType(), ride.getAppliedDiscount());

        ride.complete(fareBreakdown); // throws if already ended (decision #12)

        Driver driver = driverRepository.findById(ride.getDriverId())
                .orElseThrow(() -> new DomainException("No driver found with id: " + ride.getDriverId()));
        driver.release();
        driver.getCab().setLocation(ride.getDropLocation());

        return fareBreakdown;
    }

    public Map<RideStatus, List<Ride>> getUserHistory(String userId) {
        return splitByStatus(rideRepository.findByUserId(userId));
    }

    public Map<RideStatus, List<Ride>> getDriverHistory(String driverId) {
        return splitByStatus(rideRepository.findByDriverId(driverId));
    }

    private Map<RideStatus, List<Ride>> splitByStatus(List<Ride> rides) {
        return rides.stream().collect(Collectors.groupingBy(Ride::getStatus));
    }

    /** Requested type first; HATCHBACK also falls back to SEDAN (decision #10). */
    private List<CarType> carTypesToTry(CarType requestedCarType) {
        if (requestedCarType == CarType.HATCHBACK) {
            return List.of(CarType.HATCHBACK, CarType.SEDAN);
        }
        return List.of(requestedCarType);
    }
}
