package com.ridehailing.model;

import java.util.Optional;

public enum CarType {
    SEDAN,
    HATCHBACK {
        @Override
        public Optional<CarType> upgradeTo() {
            return Optional.of(SEDAN);
        }
    };

    /**
     * The next car type to try if no driver of this one is available within radius
     * (decision #10). Empty means there's nothing to upgrade to. A new car type that
     * should also fall back to something just overrides this method the same way
     * HATCHBACK does.
     */
    public Optional<CarType> upgradeTo() {
        return Optional.empty();
    }
}
