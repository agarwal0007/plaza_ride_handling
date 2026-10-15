package com.ridehailing.model;

import com.ridehailing.exception.DomainException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A registered driver and their cab. Status is held in an AtomicReference so that
 * claiming a driver for a ride is a single compare-and-swap (decision #11) - two
 * threads racing to book the same driver can't both succeed.
 */
public class Driver {
    private final String driverId;
    private final String name;
    private final Cab cab;
    private final double rating;
    private final AtomicReference<DriverStatus> status;

    public Driver(String driverId, String name, Cab cab, double rating) {
        if (rating < 1.0 || rating > 5.0) {
            throw new DomainException("Driver rating must be between 1.0 and 5.0, got: " + rating);
        }
        this.driverId = driverId;
        this.name = name;
        this.cab = cab;
        this.rating = rating;
        this.status = new AtomicReference<>(DriverStatus.AVAILABLE);
    }

    public String getDriverId() {
        return driverId;
    }

    public String getName() {
        return name;
    }

    public Cab getCab() {
        return cab;
    }

    public double getRating() {
        return rating;
    }

    public DriverStatus getStatus() {
        return status.get();
    }

    /** Atomically claims this driver for a ride. Returns false if it wasn't AVAILABLE. */
    public boolean claim() {
        return status.compareAndSet(DriverStatus.AVAILABLE, DriverStatus.ON_TRIP);
    }

    /** Releases this driver back to AVAILABLE after their ride ends. */
    public void release() {
        status.set(DriverStatus.AVAILABLE);
    }
}
