package com.ridehailing.repo;

import com.ridehailing.model.Driver;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDriverRepository implements DriverRepository {
    private final Map<String, Driver> drivers = new ConcurrentHashMap<>();

    @Override
    public void save(Driver driver) {
        drivers.put(driver.getDriverId(), driver);
    }

    @Override
    public Optional<Driver> findById(String driverId) {
        return Optional.ofNullable(drivers.get(driverId));
    }

    @Override
    public Collection<Driver> findAll() {
        return drivers.values();
    }
}
