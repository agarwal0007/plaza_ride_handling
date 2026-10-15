package com.ridehailing.model;

import com.ridehailing.discount.DiscountStrategy;
import com.ridehailing.exception.DomainException;
import java.time.Instant;

/**
 * A single ride: its request, its assignment, and (once ended or cancelled) its
 * outcome. Pricing always uses requestedCarType, never assignedCarType - that's how
 * the HATCHBACK-requested-but-SEDAN-assigned free upgrade works (decision #5).
 * surgeBps is likewise locked in at booking and never recomputed - the rider agreed
 * to that multiplier when they booked, so a later demand change doesn't change their
 * fare. bookedAt anchors the cancellation grace period (Phase 12): it's always the
 * instant RideService actually created this Ride, from its injected Clock - never
 * read from the system clock directly.
 */
public class Ride {
    /** 10000 bps = 1.0x. The default for a ride booked before surge pricing existed, or under NoSurge. */
    public static final int NO_SURGE_BPS = 10_000;

    private final String rideId;
    private final String userId;
    private final String driverId;
    private final CarType requestedCarType;
    private final CarType assignedCarType;
    private final Location pickupLocation;
    private final Location dropLocation;
    private final int surgeBps;
    private final Instant bookedAt;
    private RideStatus status;
    private FareBreakdown fareBreakdown;
    private long cancellationFeePaise;
    private String appliedCouponCode;
    private DiscountStrategy appliedDiscount;

    public Ride(String rideId, String userId, String driverId, CarType requestedCarType,
                CarType assignedCarType, Location pickupLocation, Location dropLocation) {
        this(rideId, userId, driverId, requestedCarType, assignedCarType, pickupLocation, dropLocation, NO_SURGE_BPS);
    }

    public Ride(String rideId, String userId, String driverId, CarType requestedCarType,
                CarType assignedCarType, Location pickupLocation, Location dropLocation, int surgeBps) {
        this(rideId, userId, driverId, requestedCarType, assignedCarType, pickupLocation, dropLocation,
                surgeBps, Instant.now());
    }

    public Ride(String rideId, String userId, String driverId, CarType requestedCarType,
                CarType assignedCarType, Location pickupLocation, Location dropLocation,
                int surgeBps, Instant bookedAt) {
        this.rideId = rideId;
        this.userId = userId;
        this.driverId = driverId;
        this.requestedCarType = requestedCarType;
        this.assignedCarType = assignedCarType;
        this.pickupLocation = pickupLocation;
        this.dropLocation = dropLocation;
        this.surgeBps = surgeBps;
        this.bookedAt = bookedAt;
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

    /** Locked at booking time (the surge decision); never recomputed when the ride ends. */
    public int getSurgeBps() {
        return surgeBps;
    }

    /** The instant RideService created this Ride - from its injected Clock, not the system clock. */
    public Instant getBookedAt() {
        return bookedAt;
    }

    public RideStatus getStatus() {
        return status;
    }

    public FareBreakdown getFareBreakdown() {
        return fareBreakdown;
    }

    /** Only meaningful once the ride is CANCELLED; 0 otherwise. */
    public long getCancellationFeePaise() {
        return cancellationFeePaise;
    }

    public String getAppliedCouponCode() {
        return appliedCouponCode;
    }

    public DiscountStrategy getAppliedDiscount() {
        return appliedDiscount;
    }

    /**
     * Locks a validated coupon onto this ride at booking time (decision #6), storing
     * the discount logic itself - not just the code - so a later coupon deletion has
     * no effect on this ride.
     */
    public void lockCoupon(String couponCode, DiscountStrategy discount) {
        this.appliedCouponCode = couponCode;
        this.appliedDiscount = discount;
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

    /**
     * Marks the ride CANCELLED with its cancellation fee. Only an ONGOING ride can be
     * cancelled - synchronized for the same reason complete() is: two concurrent
     * attempts on the same ride (a cancel racing an end, or two cancels) can't both
     * succeed.
     */
    public synchronized void cancel(long cancellationFeePaise) {
        if (status != RideStatus.ONGOING) {
            throw new DomainException("Ride " + rideId + " cannot be cancelled (status: " + status + ").");
        }
        this.status = RideStatus.CANCELLED;
        this.cancellationFeePaise = cancellationFeePaise;
    }
}
