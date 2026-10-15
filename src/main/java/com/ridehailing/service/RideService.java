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
import com.ridehailing.surge.SurgeStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Books and ends rides. Deliberately has no pricing math, driver-selection, or surge
 * logic of its own - it delegates those to PricingEngine, DriverMatchingStrategy, and
 * SurgeStrategy, all injected via the constructor. The one business rule it does own
 * is walking the upgrade chain (decision #10: that's a pricing/business decision, not
 * a matching concern) - try the requested car type, then whatever CarType.upgradeTo()
 * says to try next, and so on, until a driver is claimed or the chain runs out. This
 * class has no idea HATCHBACK or SEDAN exist; that mapping lives entirely on CarType.
 */
public class RideService {
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final CouponService couponService;
    private final PricingEngine pricingEngine;
    private final DriverMatchingStrategy matchingStrategy;
    private final SurgeStrategy surgeStrategy;
    private final double radiusKm;

    public RideService(UserRepository userRepository,
                        DriverRepository driverRepository,
                        RideRepository rideRepository,
                        CouponService couponService,
                        PricingEngine pricingEngine,
                        DriverMatchingStrategy matchingStrategy,
                        SurgeStrategy surgeStrategy,
                        double radiusKm) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
        this.couponService = couponService;
        this.pricingEngine = pricingEngine;
        this.matchingStrategy = matchingStrategy;
        this.surgeStrategy = surgeStrategy;
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

        // Computed before any driver is claimed, so the driver this booking is about
        // to claim doesn't skew its own supply count (the surge decision).
        int surgeBps = surgeStrategy.multiplierBps(pickupLocation, requestedCarType);

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
                assignedCarType, pickupLocation, dropLocation, surgeBps);
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
                distanceKm, ride.getRequestedCarType(), ride.getSurgeBps(), ride.getAppliedDiscount());

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

    /** Requested type first, then each successive CarType.upgradeTo() (decision #10). */
    private List<CarType> carTypesToTry(CarType requestedCarType) {
        List<CarType> chain = new ArrayList<>();
        Optional<CarType> current = Optional.of(requestedCarType);
        while (current.isPresent()) {
            CarType carType = current.get();
            chain.add(carType);
            current = carType.upgradeTo();
        }
        return chain;
    }
}
