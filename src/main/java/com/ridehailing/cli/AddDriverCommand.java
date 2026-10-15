package com.ridehailing.cli;

import com.ridehailing.model.CarType;
import com.ridehailing.model.Driver;
import com.ridehailing.service.DriverService;

/** add_driver <driverId> <name> <SEDAN|HATCHBACK> <plate> <x> <y> [rating] */
public class AddDriverCommand implements Command {
    private final DriverService driverService;

    public AddDriverCommand(DriverService driverService) {
        this.driverService = driverService;
    }

    @Override
    public void execute(String[] args) {
        String driverId = args[0];
        String name = args[1];
        CarType carType = CarType.valueOf(args[2].toUpperCase());
        String plate = args[3];
        double x = Double.parseDouble(args[4]);
        double y = Double.parseDouble(args[5]);

        Driver driver = args.length > 6
                ? driverService.registerDriver(driverId, name, carType, plate, x, y, Double.parseDouble(args[6]))
                : driverService.registerDriver(driverId, name, carType, plate, x, y);

        System.out.println("Registered driver " + driverId + " (" + name + ", " + carType
                + ", " + plate + ") at (" + x + ", " + y + "), rating " + driver.getRating());
    }
}
