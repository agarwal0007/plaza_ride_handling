package com.ridehailing.model;

/**
 * Itemized result of ending a ride (decision #4, extended by the surge decision in
 * Phase 11): tiered fare -> minimum-fare floor -> surge -> coupon discount -> floor
 * at 0. All money fields are in paise except distanceKm.
 */
public record FareBreakdown(
        double distanceKm,
        long tieredFare,
        long minimumFareAdjustment,
        long surgeAdjustment,
        long discount,
        long total,
        CarType pricedAsCarType
) {
}
