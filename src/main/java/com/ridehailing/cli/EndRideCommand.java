package com.ridehailing.cli;

import com.ridehailing.model.FareBreakdown;
import com.ridehailing.service.RideService;

/** end_ride <rideId> */
public class EndRideCommand implements Command {
    private final RideService rideService;

    public EndRideCommand(RideService rideService) {
        this.rideService = rideService;
    }

    @Override
    public void execute(String[] args) {
        String rideId = args[0];
        FareBreakdown fare = rideService.endRide(rideId);

        System.out.println("Ride " + rideId + " ended. Fare breakdown:");
        System.out.println("  Distance:            " + fare.distanceKm() + " km");
        System.out.println("  Priced as:           " + fare.pricedAsCarType());
        System.out.println("  Tiered fare:         " + Money.format(fare.tieredFare()));
        System.out.println("  Minimum-fare top-up: " + Money.format(fare.minimumFareAdjustment()));
        System.out.println("  Coupon discount:     -" + Money.format(fare.discount()));
        System.out.println("  Total:               " + Money.format(fare.total()));
    }
}
