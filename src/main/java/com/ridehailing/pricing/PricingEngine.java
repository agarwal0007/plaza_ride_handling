package com.ridehailing.pricing;

import com.ridehailing.discount.DiscountStrategy;
import com.ridehailing.model.CarType;
import com.ridehailing.model.FareBreakdown;

/**
 * Computes a ride's fare: tiered distance fare -> minimum-fare floor -> surge ->
 * coupon discount -> floor the total at 0 (decision #4, extended with surge in
 * Phase 11). Callers decide which car type to price as (decision #5 - always the
 * requested type, never the assigned one); this class doesn't know or care about
 * that distinction.
 */
public class PricingEngine {
    /** 10000 bps = 1.0x, i.e. no surge. */
    private static final int NO_SURGE_BPS = 10_000;

    private final RateCardProvider rateCardProvider;

    public PricingEngine(RateCardProvider rateCardProvider) {
        this.rateCardProvider = rateCardProvider;
    }

    public FareBreakdown price(double distanceKm, CarType pricedAsCarType, DiscountStrategy discount) {
        return price(distanceKm, pricedAsCarType, NO_SURGE_BPS, discount);
    }

    public FareBreakdown price(double distanceKm, CarType pricedAsCarType, int surgeBps, DiscountStrategy discount) {
        RateCard rateCard = rateCardProvider.getRateCard(pricedAsCarType);

        long tieredFare = tieredFare(distanceKm, rateCard);
        long minimumFareAdjustment = Math.max(0, rateCard.minimumFarePaise() - tieredFare);
        long afterMinimumFloor = tieredFare + minimumFareAdjustment;

        long afterSurge = applyBps(afterMinimumFloor, surgeBps);
        long surgeAdjustment = afterSurge - afterMinimumFloor;

        long discountAmount = discount == null ? 0 : discount.calculateDiscount(afterSurge);
        long total = Math.max(0, afterSurge - discountAmount);

        return new FareBreakdown(distanceKm, tieredFare, minimumFareAdjustment, surgeAdjustment,
                discountAmount, total, pricedAsCarType);
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

    /** amountPaise * bps / 10000, in long arithmetic only, rounded half-up. */
    private long applyBps(long amountPaise, int bps) {
        long numerator = amountPaise * bps;
        long quotient = numerator / 10_000;
        long remainder = numerator % 10_000;
        if (remainder * 2 >= 10_000) {
            quotient++;
        }
        return quotient;
    }
}
