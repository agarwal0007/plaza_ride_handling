package com.ridehailing.matching;

import com.ridehailing.model.CarType;
import com.ridehailing.model.Driver;
import com.ridehailing.model.DriverStatus;
import com.ridehailing.model.Location;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AVAILABLE drivers of the requested car type within the radius, highest rating
 * first, ties broken by distance ascending. Returns a sorted list rather than a
 * single pick, same as NearestDriverStrategy, so the claim/retry loop in
 * RideService keeps working unchanged regardless of which strategy is wired in.
 */
public class HighestRatedStrategy implements DriverMatchingStrategy {

    @Override
    public List<Driver> findCandidates(CarType carType, Location pickupLocation, double radiusKm, Collection<Driver> allDrivers) {
        return allDrivers.stream()
                .filter(driver -> driver.getStatus() == DriverStatus.AVAILABLE)
                .filter(driver -> driver.getCab().getCarType() == carType)
                .filter(driver -> driver.getCab().getLocation().distanceTo(pickupLocation) <= radiusKm)
                .sorted(Comparator
                        .comparingDouble(Driver::getRating).reversed()
                        .thenComparingDouble(driver -> driver.getCab().getLocation().distanceTo(pickupLocation)))
                .collect(Collectors.toList());
    }
}
