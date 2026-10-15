package com.ridehailing.repo;

import com.ridehailing.model.Ride;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryRideRepository implements RideRepository {
    private final Map<String, Ride> rides = new ConcurrentHashMap<>();

    @Override
    public void save(Ride ride) {
        rides.put(ride.getRideId(), ride);
    }

    @Override
    public Optional<Ride> findById(String rideId) {
        return Optional.ofNullable(rides.get(rideId));
    }

    @Override
    public List<Ride> findByUserId(String userId) {
        return rides.values().stream()
                .filter(ride -> ride.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Ride> findByDriverId(String driverId) {
        return rides.values().stream()
                .filter(ride -> ride.getDriverId().equals(driverId))
                .collect(Collectors.toList());
    }
}
