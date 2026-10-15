package com.ridehailing.cli;

import com.ridehailing.service.CouponService;

/** delete_coupon <code> */
public class DeleteCouponCommand implements Command {
    private final CouponService couponService;

    public DeleteCouponCommand(CouponService couponService) {
        this.couponService = couponService;
    }

    @Override
    public void execute(String[] args) {
        String code = args[0];
        couponService.deleteCoupon(code);
        System.out.println("Deleted coupon " + code);
    }
}
