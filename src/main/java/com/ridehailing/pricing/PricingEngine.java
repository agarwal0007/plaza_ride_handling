package com.ridehailing.pricing;

import com.ridehailing.discount.DiscountStrategy;
import com.ridehailing.model.CarType;
import com.ridehailing.model.FareBreakdown;

/**
 * Computes a ride's fare: tiered distance fare -> minimum-fare floor -> coupon
 * discount -> floor the total at 0 (decision #4). Callers decide which car type to
 * price as (decision #5 - always the requested type, never the assigned one); this
 * class doesn't know or care about that distinction.
 */
public class PricingEngine {
    private final RateCardProvider rateCardProvider;

    public PricingEngine(RateCardProvider rateCardProvider) {
        this.rateCardProvider = rateCardProvider;
    }

    public FareBreakdown price(double distanceKm, CarType pricedAsCarType, DiscountStrategy discount) {
        RateCard rateCard = rateCardProvider.getRateCard(pricedAsCarType);

        long tieredFare = tieredFare(distanceKm, rateCard);
        long minimumFareAdjustment = Math.max(0, rateCard.minimumFarePaise() - tieredFare);
        long afterMinimumFloor = tieredFare + minimumFareAdjustment;

        long discountAmount = discount == null ? 0 : discount.calculateDiscount(afterMinimumFloor);
        long total = Math.max(0, afterMinimumFloor - discountAmount);

        return new FareBreakdown(distanceKm, tieredFare, minimumFareAdjustment, discountAmount, total, pricedAsCarType);
    }

    /** Sums each tier's rate times however much of the distance falls in that tier. */
    private long tieredFare(double distanceKm, RateCard rateCard) {
        long fare = 0;
        double previousBoundary = 0;
        double remaining = distanceKm;

        for (Tier tier : rateCard.tiers()) {
            if (remaining <= 0) {
                break;
            }
            double tierSpan = tier.upToKm() - previousBoundary;
            double kmInThisTier = Math.min(remaining, tierSpan);
            fare += Math.round(kmInThisTier * tier.ratePerKmPaise());
            remaining -= kmInThisTier;
            previousBoundary = tier.upToKm();
        }

        return fare;
    }
}
