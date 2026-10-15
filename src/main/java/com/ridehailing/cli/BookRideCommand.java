package com.ridehailing.cli;

import com.ridehailing.model.CarType;
import com.ridehailing.model.Location;
import com.ridehailing.model.Ride;
import com.ridehailing.service.RideService;

/** book_ride <rideId> <userId> <SEDAN|HATCHBACK> <px> <py> <dx> <dy> [couponCode] */
public class BookRideCommand implements Command {
    private final RideService rideService;

    public BookRideCommand(RideService rideService) {
        this.rideService = rideService;
    }

    @Override
    public void execute(String[] args) {
        String rideId = args[0];
        String userId = args[1];
        CarType requestedCarType = CarType.valueOf(args[2].toUpperCase());
        Location pickup = new Location(Double.parseDouble(args[3]), Double.parseDouble(args[4]));
        Location drop = new Location(Double.parseDouble(args[5]), Double.parseDouble(args[6]));
        String couponCode = args.length > 7 ? args[7] : null;

        Ride ride = rideService.book(rideId, userId, requestedCarType, pickup, drop, couponCode);

        boolean upgraded = ride.getAssignedCarType() != ride.getRequestedCarType();
        System.out.println("Booked ride " + rideId + ": driver " + ride.getDriverId()
                + ", car type " + ride.getAssignedCarType() + (upgraded ? " (free upgrade)" : ""));
    }
}
