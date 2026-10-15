package com.ridehailing.model;

/**
 * Itemized result of ending a ride (decision #4: tiered fare -> minimum-fare floor ->
 * coupon discount -> floor at 0). All money fields are in paise except distanceKm.
 */
public record FareBreakdown(
        double distanceKm,
        long tieredFare,
        long minimumFareAdjustment,
        long discount,
        long total,
        CarType pricedAsCarType
) {
}
