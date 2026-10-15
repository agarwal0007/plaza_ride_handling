package com.ridehailing.surge;

import com.ridehailing.model.Cab;
import com.ridehailing.model.CarType;
import com.ridehailing.model.Driver;
import com.ridehailing.model.Location;
import com.ridehailing.model.Ride;
import com.ridehailing.repo.DriverRepository;
import com.ridehailing.repo.InMemoryDriverRepository;
import com.ridehailing.repo.InMemoryRideRepository;
import com.ridehailing.repo.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DemandSupplySurgeTest {

    private static final double RADIUS_KM = 5.0;

    private DriverRepository driverRepository;
    private RideRepository rideRepository;
    private DemandSupplySurge surgeStrategy;

    @BeforeEach
    void setUp() {
        driverRepository = new InMemoryDriverRepository();
        rideRepository = new InMemoryRideRepository();
        surgeStrategy = new DemandSupplySurge(driverRepository, rideRepository, RADIUS_KM);
    }

    private void addAvailableDriver(String id) {
        Cab cab = new Cab(CarType.SEDAN, "PLATE-" + id, new Location(0, 0));
        driverRepository.save(new Driver(id, "Driver " + id, cab, 5.0));
    }

    private void addOngoingRideAt(String rideId, Location pickup) {
        rideRepository.save(new Ride(rideId, "u-" + rideId, "d-" + rideId, CarType.SEDAN, CarType.SEDAN,
                pickup, new Location(100, 100)));
    }

    @Test
    void ratioBelowOneMeansNoSurge() {
        addAvailableDriver("d1");
        addAvailableDriver("d2"); // supply = 2
        addOngoingRideAt("r1", new Location(0, 0)); // demand = 1, ratio = 0.5
        assertEquals(10_000, surgeStrategy.multiplierBps(new Location(0, 0), CarType.SEDAN));
    }

    @Test
    void ratioExactlyOneHitsTheMidBand() {
        addAvailableDriver("d1"); // supply = 1
        addOngoingRideAt("r1", new Location(0, 0)); // demand = 1, ratio = 1
        assertEquals(12_500, surgeStrategy.multiplierBps(new Location(0, 0), CarType.SEDAN));
    }

    @Test
    void ratioExactlyTwoHitsTheHighBand() {
        addAvailableDriver("d1"); // supply = 1
        addOngoingRideAt("r1", new Location(0, 0));
        addOngoingRideAt("r2", new Location(0, 0)); // demand = 2, ratio = 2
        assertEquals(15_000, surgeStrategy.multiplierBps(new Location(0, 0), CarType.SEDAN));
    }

    @Test
    void zeroSupplyAndZeroDemandMeansNoSurge() {
        assertEquals(10_000, surgeStrategy.multiplierBps(new Location(0, 0), CarType.SEDAN));
    }

    @Test
    void zeroSupplyWithDemandDoesNotDivideByZero() {
        // supply = 0, demand = 1 -> ratio = 1 / max(0, 1) = 1, not a division failure.
        addOngoingRideAt("r1", new Location(0, 0));
        assertEquals(12_500, surgeStrategy.multiplierBps(new Location(0, 0), CarType.SEDAN));
    }

    @Test
    void ridesOutsideTheRadiusDoNotCountAsDemand() {
        addAvailableDriver("d1"); // within radius of the query pickup
        addOngoingRideAt("r1", new Location(100, 100)); // its pickup is far outside radius
        assertEquals(10_000, surgeStrategy.multiplierBps(new Location(0, 0), CarType.SEDAN));
    }
}
