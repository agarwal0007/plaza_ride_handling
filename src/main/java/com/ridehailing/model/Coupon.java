package com.ridehailing.model;

import com.ridehailing.discount.DiscountStrategy;

/** A coupon code paired with the discount logic it applies (decision #7). */
public class Coupon {
    private final String code;
    private final DiscountStrategy discountStrategy;

    public Coupon(String code, DiscountStrategy discountStrategy) {
        this.code = code;
        this.discountStrategy = discountStrategy;
    }

    public String getCode() {
        return code;
    }

    public DiscountStrategy getDiscountStrategy() {
        return discountStrategy;
    }
}
