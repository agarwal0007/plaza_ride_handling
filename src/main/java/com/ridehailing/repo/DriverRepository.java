package com.ridehailing.repo;

import com.ridehailing.model.Driver;
import java.util.Collection;
import java.util.Optional;

public interface DriverRepository {
    void save(Driver driver);

    Optional<Driver> findById(String driverId);

    /** All registered drivers - the matching strategy scans these for candidates. */
    Collection<Driver> findAll();
}
