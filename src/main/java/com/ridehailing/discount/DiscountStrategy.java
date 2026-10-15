package com.ridehailing.discount;

/**
 * Computes a discount, in paise, for a given fare (also in paise). One method, so a new
 * discount type (e.g. a flat rupees-off coupon) is just one new class, with nothing
 * else - coupon and pricing code only ever depend on this interface (decision #7).
 */
public interface DiscountStrategy {
    long calculateDiscount(long farePaise);
}
