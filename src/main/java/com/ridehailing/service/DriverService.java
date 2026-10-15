package com.ridehailing.service;

import com.ridehailing.exception.DomainException;
import com.ridehailing.model.Cab;
import com.ridehailing.model.CarType;
import com.ridehailing.model.Driver;
import com.ridehailing.model.Location;
import com.ridehailing.repo.DriverRepository;

public class DriverService {
    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public Driver registerDriver(String driverId, String name, CarType carType, String plate, double x, double y) {
        Cab cab = new Cab(carType, plate, new Location(x, y));
        Driver driver = new Driver(driverId, name, cab);
        driverRepository.save(driver);
        return driver;
    }

    public void updateLocation(String driverId, double x, double y) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new DomainException("No driver registered with id: " + driverId));
        driver.getCab().setLocation(new Location(x, y));
    }
}
