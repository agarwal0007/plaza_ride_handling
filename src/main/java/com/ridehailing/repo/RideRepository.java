package com.ridehailing.repo;

import com.ridehailing.model.Ride;
import java.util.List;
import java.util.Optional;

public interface RideRepository {
    void save(Ride ride);

    Optional<Ride> findById(String rideId);

    /** Every ride ever created, any status - surge demand scans all of them by status/pickup. */
    List<Ride> findAll();

    /** All rides for a user, both ongoing and completed - the service splits by status. */
    List<Ride> findByUserId(String userId);

    /** All rides for a driver, both ongoing and completed - the service splits by status. */
    List<Ride> findByDriverId(String driverId);
}
