package com.ridehailing.cli;

import com.ridehailing.service.DriverService;

/** update_location <driverId> <x> <y> */
public class UpdateLocationCommand implements Command {
    private final DriverService driverService;

    public UpdateLocationCommand(DriverService driverService) {
        this.driverService = driverService;
    }

    @Override
    public void execute(String[] args) {
        String driverId = args[0];
        double x = Double.parseDouble(args[1]);
        double y = Double.parseDouble(args[2]);

        driverService.updateLocation(driverId, x, y);
        System.out.println("Updated " + driverId + "'s location to (" + x + ", " + y + ")");
    }
}
