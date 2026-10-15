package com.ridehailing.surge;

import com.ridehailing.model.CarType;
import com.ridehailing.model.DriverStatus;
import com.ridehailing.model.Location;
import com.ridehailing.model.RideStatus;
import com.ridehailing.repo.DriverRepository;
import com.ridehailing.repo.RideRepository;

/**
 * Surges based on how many ongoing rides (demand) are competing for how many
 * available drivers of the requested type (supply), within the booking radius of the
 * pickup. ratio = demand / max(supply, 1) - the max(supply, 1) guard means zero
 * supply never divides by zero, it just always lands in the highest band that the
 * demand alone qualifies for.
 */
public class DemandSupplySurge implements SurgeStrategy {
    private static final double DEFAULT_MID_RATIO_THRESHOLD = 1.0;
    private static final double DEFAULT_HIGH_RATIO_THRESHOLD = 2.0;
    private static final int DEFAULT_MID_MULTIPLIER_BPS = 12_500;
    private static final int DEFAULT_HIGH_MULTIPLIER_BPS = 15_000;
    private static final int NO_SURGE_BPS = 10_000;

    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final double radiusKm;
    private final double midRatioThreshold;
    private final double highRatioThreshold;
    private final int midMultiplierBps;
    private final int highMultiplierBps;

    public DemandSupplySurge(DriverRepository driverRepository, RideRepository rideRepository, double radiusKm) {
        this(driverRepository, rideRepository, radiusKm,
                DEFAULT_MID_RATIO_THRESHOLD, DEFAULT_HIGH_RATIO_THRESHOLD,
                DEFAULT_MID_MULTIPLIER_BPS, DEFAULT_HIGH_MULTIPLIER_BPS);
    }

    public DemandSupplySurge(DriverRepository driverRepository, RideRepository rideRepository, double radiusKm,
                              double midRatioThreshold, double highRatioThreshold,
                              int midMultiplierBps, int highMultiplierBps) {
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
        this.radiusKm = radiusKm;
        this.midRatioThreshold = midRatioThreshold;
        this.highRatioThreshold = highRatioThreshold;
        this.midMultiplierBps = midMultiplierBps;
        this.highMultiplierBps = highMultiplierBps;
    }

    @Override
    public int multiplierBps(Location pickup, CarType type) {
        long supply = driverRepository.findAll().stream()
                .filter(driver -> driver.getStatus() == DriverStatus.AVAILABLE)
                .filter(driver -> driver.getCab().getCarType() == type)
                .filter(driver -> driver.getCab().getLocation().distanceTo(pickup) <= radiusKm)
                .count();

        long demand = rideRepository.findAll().stream()
                .filter(ride -> ride.getStatus() == RideStatus.ONGOING)
                .filter(ride -> ride.getPickupLocation().distanceTo(pickup) <= radiusKm)
                .count();

        double ratio = (double) demand / Math.max(supply, 1);

        if (ratio >= highRatioThreshold) {
            return highMultiplierBps;
        }
        if (ratio >= midRatioThreshold) {
            return midMultiplierBps;
        }
        return NO_SURGE_BPS;
    }
}
