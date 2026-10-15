package com.ridehailing.model;

import com.ridehailing.exception.DomainException;

/**
 * A single ride: its request, its assignment, and (once ended) its fare.
 * Pricing always uses requestedCarType, never assignedCarType - that's how the
 * HATCHBACK-requested-but-SEDAN-assigned free upgrade works (decision #5).
 */
public class Ride {
    private final String rideId;
    private final String userId;
    private final String driverId;
    private final CarType requestedCarType;
    private final CarType assignedCarType;
    private final Location pickupLocation;
    private final Location dropLocation;
    private RideStatus status;
    private FareBreakdown fareBreakdown;

    public Ride(String rideId, String userId, String driverId, CarType requestedCarType,
                CarType assignedCarType, Location pickupLocation, Location dropLocation) {
        this.rideId = rideId;
        this.userId = userId;
        this.driverId = driverId;
        this.requestedCarType = requestedCarType;
        this.assignedCarType = assignedCarType;
        this.pickupLocation = pickupLocation;
        this.dropLocation = dropLocation;
        this.status = RideStatus.ONGOING;
    }

    public String getRideId() {
        return rideId;
    }

    public String getUserId() {
        return userId;
    }

    public String getDriverId() {
        return driverId;
    }

    public CarType getRequestedCarType() {
        return requestedCarType;
    }

    public CarType getAssignedCarType() {
        return assignedCarType;
    }

    public Location getPickupLocation() {
        return pickupLocation;
    }

    public Location getDropLocation() {
        return dropLocation;
    }

    public RideStatus getStatus() {
        return status;
    }

    public FareBreakdown getFareBreakdown() {
        return fareBreakdown;
    }

    /**
     * Marks the ride COMPLETED with its fare. Synchronized so two concurrent end_ride
     * calls on the same ride can't both succeed (decision #12).
     */
    public synchronized void complete(FareBreakdown fareBreakdown) {
        if (status != RideStatus.ONGOING) {
            throw new DomainException("Ride " + rideId + " has already ended.");
        }
        this.status = RideStatus.COMPLETED;
        this.fareBreakdown = fareBreakdown;
    }
}
