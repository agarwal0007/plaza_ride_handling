package com.ridehailing.model;

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
    private final AtomicReference<DriverStatus> status;

    public Driver(String driverId, String name, Cab cab) {
        this.driverId = driverId;
        this.name = name;
        this.cab = cab;
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
