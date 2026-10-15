package com.ridehailing.matching;

import com.ridehailing.model.CarType;
import com.ridehailing.model.Driver;
import com.ridehailing.model.Location;
import java.util.Collection;
import java.util.List;

/**
 * Finds candidate drivers for a ride request. Returns a list, nearest first, rather
 * than a single pick: claiming a driver can fail under concurrency (decision #11), so
 * the caller tries the next candidate if the nearest one was just claimed by someone else.
 */
public interface DriverMatchingStrategy {
    List<Driver> findCandidates(CarType carType, Location pickupLocation, double radiusKm, Collection<Driver> allDrivers);
}
