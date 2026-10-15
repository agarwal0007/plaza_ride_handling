package com.ridehailing.repo;

import com.ridehailing.model.Coupon;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCouponRepository implements CouponRepository {
    private final Map<String, Coupon> coupons = new ConcurrentHashMap<>();

    @Override
    public void save(Coupon coupon) {
        coupons.put(coupon.getCode(), coupon);
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
        return Optional.ofNullable(coupons.get(code));
    }

    @Override
    public void delete(String code) {
        coupons.remove(code);
    }
}
