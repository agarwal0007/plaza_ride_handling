package com.ridehailing.pricing;

import java.util.List;

/**
 * A car type's full pricing rule: its ordered distance tiers (first slab first, last
 * tier's upToKm is infinite) plus its minimum fare.
 */
public record RateCard(List<Tier> tiers, long minimumFarePaise) {
}
