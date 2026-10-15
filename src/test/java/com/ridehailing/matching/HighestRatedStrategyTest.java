package com.ridehailing.matching;

import com.ridehailing.model.Cab;
import com.ridehailing.model.CarType;
import com.ridehailing.model.Driver;
import com.ridehailing.model.Location;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HighestRatedStrategyTest {

    private static final double RADIUS_KM = 5.0;

    private Driver driver(String id, double rating, double x, double y) {
        Cab cab = new Cab(CarType.SEDAN, "PLATE-" + id, new Location(x, y));
        return new Driver(id, "Driver " + id, cab, rating);
    }

    @Test
    void nearestAndRatedStrategiesPickDifferentDriversForTheSameRequest() {
        // "close" is nearer but rated lower; "far" is farther but rated higher - both
        // within radius, so the two strategies genuinely disagree on who's best.
        Driver close = driver("close", 3.0, 1, 0);
        Driver far = driver("far", 4.9, 4, 0);
        List<Driver> drivers = List.of(close, far);

        List<Driver> nearestOrder = new NearestDriverStrategy()
                .findCandidates(CarType.SEDAN, new Location(0, 0), RADIUS_KM, drivers);
        List<Driver> ratedOrder = new HighestRatedStrategy()
                .findCandidates(CarType.SEDAN, new Location(0, 0), RADIUS_KM, drivers);

        assertEquals("close", nearestOrder.get(0).getDriverId());
        assertEquals("far", ratedOrder.get(0).getDriverId());
    }

    @Test
    void tiedRatingsAreBrokenByDistanceAscending() {
        Driver farButSameRating = driver("far", 4.0, 3, 0);
        Driver nearButSameRating = driver("near", 4.0, 1, 0);
        List<Driver> drivers = List.of(farButSameRating, nearButSameRating);

        List<Driver> candidates = new HighestRatedStrategy()
                .findCandidates(CarType.SEDAN, new Location(0, 0), RADIUS_KM, drivers);

        assertEquals("near", candidates.get(0).getDriverId());
        assertEquals("far", candidates.get(1).getDriverId());
    }
}
