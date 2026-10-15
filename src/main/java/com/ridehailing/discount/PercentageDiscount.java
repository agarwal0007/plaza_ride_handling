package com.ridehailing.discount;

/** Percent-off discount, capped at a maximum paise amount. */
public class PercentageDiscount implements DiscountStrategy {
    private final int percent;
    private final long maxCapPaise;

    public PercentageDiscount(int percent, long maxCapPaise) {
        this.percent = percent;
        this.maxCapPaise = maxCapPaise;
    }

    @Override
    public long calculateDiscount(long farePaise) {
        long rawDiscount = Math.round(farePaise * percent / 100.0);
        return Math.min(rawDiscount, maxCapPaise);
    }
}
