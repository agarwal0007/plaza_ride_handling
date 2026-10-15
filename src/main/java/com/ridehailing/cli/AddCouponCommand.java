package com.ridehailing.cli;

import com.ridehailing.discount.PercentageDiscount;
import com.ridehailing.exception.DomainException;
import com.ridehailing.service.CouponService;

/** add_coupon <code> PERCENT <percent> <maxCapRupees> */
public class AddCouponCommand implements Command {
    private final CouponService couponService;

    public AddCouponCommand(CouponService couponService) {
        this.couponService = couponService;
    }

    @Override
    public void execute(String[] args) {
        String code = args[0];
        String type = args[1];
        if (!type.equalsIgnoreCase("PERCENT")) {
            throw new DomainException("Unknown coupon type: " + type + " (only PERCENT is supported)");
        }
        int percent = Integer.parseInt(args[2]);
        long maxCapPaise = Math.round(Double.parseDouble(args[3]) * 100);

        couponService.addCoupon(code, new PercentageDiscount(percent, maxCapPaise));
        System.out.println("Added coupon " + code + ": " + percent + "% off, capped at " + Money.format(maxCapPaise));
    }
}
