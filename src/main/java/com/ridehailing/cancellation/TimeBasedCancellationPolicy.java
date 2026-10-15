package com.ridehailing.cancellation;

import com.ridehailing.model.Ride;
import java.time.Duration;
import java.time.Instant;

/** Free within the grace period after booking, then a flat fee. Coupons and surge never apply to this fee. */
public class TimeBasedCancellationPolicy implements CancellationFeePolicy {
    private static final Duration DEFAULT_GRACE_PERIOD = Duration.ofMinutes(2);
    private static final long DEFAULT_FLAT_FEE_PAISE = 2_500; // Rs 25

    private final Duration gracePeriod;
    private final long flatFeePaise;

    public TimeBasedCancellationPolicy() {
        this(DEFAULT_GRACE_PERIOD, DEFAULT_FLAT_FEE_PAISE);
    }

    public TimeBasedCancellationPolicy(Duration gracePeriod, long flatFeePaise) {
        this.gracePeriod = gracePeriod;
        this.flatFeePaise = flatFeePaise;
    }

    @Override
    public long feePaise(Ride ride, Instant cancelledAt) {
        Duration sinceBooking = Duration.between(ride.getBookedAt(), cancelledAt);
        // "Within" the grace period includes its own boundary - cancelling at exactly
        // the grace-period mark is still free, only strictly after it costs the fee.
        return sinceBooking.compareTo(gracePeriod) <= 0 ? 0 : flatFeePaise;
    }
}
