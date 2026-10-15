package com.ridehailing.surge;

import com.ridehailing.model.CarType;
import com.ridehailing.model.Location;

/** Never surges. The default strategy until -Dsurge=demand opts in. */
public class NoSurge implements SurgeStrategy {
    @Override
    public int multiplierBps(Location pickup, CarType type) {
        return 10_000;
    }
}
