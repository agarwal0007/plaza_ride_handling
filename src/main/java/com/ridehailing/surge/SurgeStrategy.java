package com.ridehailing.surge;

import com.ridehailing.model.CarType;
import com.ridehailing.model.Location;

/** Computes a surge multiplier for a pickup and requested car type. 10000 = 1.0x (no surge). */
public interface SurgeStrategy {
    int multiplierBps(Location pickup, CarType type);
}
