package com.ridehailing.cancellation;

import com.ridehailing.model.Ride;
import java.time.Instant;

/** Computes the fee (in paise) for cancelling a ride at a given instant. */
public interface CancellationFeePolicy {
    long feePaise(Ride ride, Instant cancelledAt);
}
