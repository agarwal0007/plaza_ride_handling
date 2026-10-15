package com.ridehailing.discount;

import com.ridehailing.exception.DomainException;
import java.util.Map;

/**
 * Builds a DiscountStrategy from a coupon TYPE name and its arguments, as the CLI's
 * add_coupon command receives them. Adding a new coupon type (e.g. a flat rupees-off
 * discount) means writing one new DiscountStrategy class and adding one entry to the
 * BUILDERS map below - nothing else in this class, or in the CLI, has to change.
 */
public class DiscountStrategyFactory {

    @FunctionalInterface
    private interface Builder {
        DiscountStrategy build(String[] args);
    }

    private static final Map<String, Builder> BUILDERS = Map.of(
            "PERCENT", DiscountStrategyFactory::buildPercentageDiscount
    );

    public DiscountStrategy create(String type, String[] args) {
        Builder builder = BUILDERS.get(type.toUpperCase());
        if (builder == null) {
            throw new DomainException("Unknown coupon type: " + type);
        }
        return builder.build(args);
    }

    private static DiscountStrategy buildPercentageDiscount(String[] args) {
        if (args.length != 2) {
            throw new DomainException("PERCENT coupon needs exactly 2 arguments: <percent> <maxCapRupees>");
        }
        int percent = Integer.parseInt(args[0]);
        long maxCapPaise = Math.round(Double.parseDouble(args[1]) * 100);
        return new PercentageDiscount(percent, maxCapPaise);
    }
}
