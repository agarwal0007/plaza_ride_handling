package com.ridehailing.repo;

import com.ridehailing.model.Coupon;
import java.util.Optional;

public interface CouponRepository {
    void save(Coupon coupon);

    Optional<Coupon> findByCode(String code);

    void delete(String code);
}
