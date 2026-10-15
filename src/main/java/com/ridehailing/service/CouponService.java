package com.ridehailing.service;

import com.ridehailing.discount.DiscountStrategy;
import com.ridehailing.exception.DomainException;
import com.ridehailing.model.Coupon;
import com.ridehailing.repo.CouponRepository;

public class CouponService {
    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public Coupon addCoupon(String code, DiscountStrategy discountStrategy) {
        Coupon coupon = new Coupon(code, discountStrategy);
        couponRepository.save(coupon);
        return coupon;
    }

    public void deleteCoupon(String code) {
        couponRepository.delete(code);
    }

    /** Looks up a coupon by code at booking time. Unknown code -> DomainException (decision #6). */
    public Coupon validate(String code) {
        return couponRepository.findByCode(code)
                .orElseThrow(() -> new DomainException("Unknown coupon code: " + code));
    }
}
