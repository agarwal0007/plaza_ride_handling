package com.ridehailing.cli;

import com.ridehailing.model.Ride;
import com.ridehailing.model.RideStatus;
import com.ridehailing.service.RideService;
import java.util.List;
import java.util.Map;

/** user_history <userId> */
public class UserHistoryCommand implements Command {
    private final RideService rideService;

    public UserHistoryCommand(RideService rideService) {
        this.rideService = rideService;
    }

    @Override
    public void execute(String[] args) {
        String userId = args[0];
        Map<RideStatus, List<Ride>> history = rideService.getUserHistory(userId);

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
