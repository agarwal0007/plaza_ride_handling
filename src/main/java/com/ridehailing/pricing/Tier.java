package com.ridehailing.pricing;

/** One slab of a tiered rate card: covers distance up to (and including) upToKm, at ratePerKmPaise. */
public record Tier(double upToKm, long ratePerKmPaise) {
}
