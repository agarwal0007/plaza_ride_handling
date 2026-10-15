package com.ridehailing.cli;

import com.ridehailing.model.Ride;
import com.ridehailing.model.RideStatus;
import com.ridehailing.service.RideService;
import java.util.List;
import java.util.Map;

/** driver_history <driverId> */
public class DriverHistoryCommand implements Command {
    private final RideService rideService;

    public DriverHistoryCommand(RideService rideService) {
        this.rideService = rideService;
    }

    @Override
    public void execute(String[] args) {
        String driverId = args[0];
        Map<RideStatus, List<Ride>> history = rideService.getDriverHistory(driverId);

        System.out.println("ONGOING:");
        for (Ride ride : history.getOrDefault(RideStatus.ONGOING, List.of())) {
            System.out.println("  " + describe(ride));
        }
        System.out.println("COMPLETED:");
        for (Ride ride : history.getOrDefault(RideStatus.COMPLETED, List.of())) {
            System.out.println("  " + describe(ride));
        }
    }

    private String describe(Ride ride) {
        String fare = ride.getFareBreakdown() == null ? "" : ", fare " + Money.format(ride.getFareBreakdown().total());
        return ride.getRideId() + " (" + ride.getRequestedCarType() + " -> " + ride.getAssignedCarType() + ")" + fare;
    }
}
