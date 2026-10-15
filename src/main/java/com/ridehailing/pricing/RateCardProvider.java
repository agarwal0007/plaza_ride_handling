package com.ridehailing.pricing;

import com.ridehailing.model.CarType;
import java.util.List;
import java.util.Map;

/**
 * The only place car-type rate data lives (decision #3). Adding a car type, or changing
 * a tier or rate for an existing one, means editing the map below and nothing else.
 */
public class RateCardProvider {
    private static final Map<CarType, RateCard> RATE_CARDS = Map.of(
            CarType.HATCHBACK, new RateCard(
                    List.of(
                            new Tier(2, 1_000),
                            new Tier(5, 800),
                            new Tier(Double.POSITIVE_INFINITY, 500)
                    ),
                    5_000
            ),
            CarType.SEDAN, new RateCard(
                    List.of(
                            new Tier(2, 1_200),
                            new Tier(5, 1_000),
                            new Tier(Double.POSITIVE_INFINITY, 700)
                    ),
                    7_000
            )
    );

    public RateCard getRateCard(CarType carType) {
        RateCard rateCard = RATE_CARDS.get(carType);
        if (rateCard == null) {
            throw new IllegalStateException("No rate card configured for car type: " + carType);
        }
        return rateCard;
    }
}
