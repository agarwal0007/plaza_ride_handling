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
        System.out.println("CANCELLED:");
        for (Ride ride : history.getOrDefault(RideStatus.CANCELLED, List.of())) {
            System.out.println("  " + describe(ride));
        }
    }

    private String describe(Ride ride) {
        String outcome;
        if (ride.getFareBreakdown() != null) {
            outcome = ", fare " + Money.format(ride.getFareBreakdown().total());
        } else if (ride.getStatus() == RideStatus.CANCELLED) {
            outcome = ", cancellation fee " + Money.format(ride.getCancellationFeePaise());
        } else {
            outcome = "";
        }
        return ride.getRideId() + " (" + ride.getRequestedCarType() + " -> " + ride.getAssignedCarType() + ")" + outcome;
    }
}
