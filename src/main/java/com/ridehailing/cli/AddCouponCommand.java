package com.ridehailing.cli;

import com.ridehailing.discount.DiscountStrategy;
import com.ridehailing.discount.DiscountStrategyFactory;
import com.ridehailing.service.CouponService;
import java.util.Arrays;

/** add_coupon <code> <TYPE> <args...> */
public class AddCouponCommand implements Command {
    private final CouponService couponService;
    private final DiscountStrategyFactory discountStrategyFactory;

    public AddCouponCommand(CouponService couponService, DiscountStrategyFactory discountStrategyFactory) {
        this.couponService = couponService;
        this.discountStrategyFactory = discountStrategyFactory;
    }

    @Override
    public void execute(String[] args) {
        String code = args[0];
        String type = args[1];
        String[] typeArgs = Arrays.copyOfRange(args, 2, args.length);

        DiscountStrategy discount = discountStrategyFactory.create(type, typeArgs);
        couponService.addCoupon(code, discount);
        System.out.println("Added coupon " + code + " (" + type.toUpperCase() + ")");
    }
}
