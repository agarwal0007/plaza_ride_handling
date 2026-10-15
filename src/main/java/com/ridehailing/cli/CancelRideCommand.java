package com.ridehailing.cli;

import com.ridehailing.service.RideService;

/** cancel_ride <rideId> */
public class CancelRideCommand implements Command {
    private final RideService rideService;

    public CancelRideCommand(RideService rideService) {
        this.rideService = rideService;
    }

    @Override
    public void execute(String[] args) {
        String rideId = args[0];
        long feePaise = rideService.cancelRide(rideId);

        if (feePaise == 0) {
            System.out.println("Ride " + rideId + " cancelled. Free cancellation.");
        } else {
            System.out.println("Ride " + rideId + " cancelled. Fee: " + Money.format(feePaise));
        }
    }
}
