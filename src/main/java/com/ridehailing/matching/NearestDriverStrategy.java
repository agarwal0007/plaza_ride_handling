package com.ridehailing.matching;

import com.ridehailing.model.CarType;
import com.ridehailing.model.Driver;
import com.ridehailing.model.DriverStatus;
import com.ridehailing.model.Location;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** AVAILABLE drivers of the requested car type within the radius, nearest first. */
public class NearestDriverStrategy implements DriverMatchingStrategy {

    @Override
    public List<Driver> findCandidates(CarType carType, Location pickupLocation, double radiusKm, Collection<Driver> allDrivers) {
        return allDrivers.stream()
                .filter(driver -> driver.getStatus() == DriverStatus.AVAILABLE)
                .filter(driver -> driver.getCab().getCarType() == carType)
                .filter(driver -> driver.getCab().getLocation().distanceTo(pickupLocation) <= radiusKm)
                .sorted(Comparator.comparingDouble(driver -> driver.getCab().getLocation().distanceTo(pickupLocation)))
                .collect(Collectors.toList());
    }
}
