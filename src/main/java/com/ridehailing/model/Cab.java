package com.ridehailing.model;

/** A driver's vehicle: its type, plate, and current position. */
public class Cab {
    private final CarType carType;
    private final String plateNumber;
    private volatile Location location;

    public Cab(CarType carType, String plateNumber, Location location) {
        this.carType = carType;
        this.plateNumber = plateNumber;
        this.location = location;
    }

    public CarType getCarType() {
        return carType;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
