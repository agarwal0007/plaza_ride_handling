package com.ridehailing.pricing;

import com.ridehailing.discount.PercentageDiscount;
import com.ridehailing.model.CarType;
import com.ridehailing.model.FareBreakdown;
import com.ridehailing.model.Location;
import com.ridehailing.model.Ride;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingEngineTest {

    private PricingEngine pricingEngine;

    @BeforeEach
    void setUp() {
        pricingEngine = new PricingEngine(new RateCardProvider());
    }

    @ParameterizedTest
    @CsvSource({
            "HATCHBACK, 2.0, 2000",
            "HATCHBACK, 5.0, 4400",
            "HATCHBACK, 7.0, 5400",
            "SEDAN, 2.0, 2400",
            "SEDAN, 5.0, 5400",
            "SEDAN, 7.0, 6800"
    })
    void tieredFareAtExactBoundaries(CarType carType, double distanceKm, long expectedTieredFare) {
        FareBreakdown breakdown = pricingEngine.price(distanceKm, carType, null);
        assertEquals(expectedTieredFare, breakdown.tieredFare());
    }

    @Test
    void sedanSevenKmHitsItsMinimumFareEvenThoughHatchbackDoesNotAtTheSameDistance() {
        // SEDAN's tiered fare at 7km (6800) is below SEDAN's own minimum fare (7000);
        // HATCHBACK's tiered fare at 7km (5400) already clears its minimum (5000).
        FareBreakdown sedan = pricingEngine.price(7.0, CarType.SEDAN, null);
        assertEquals(200, sedan.minimumFareAdjustment());
        assertEquals(7000, sedan.total());

        FareBreakdown hatchback = pricingEngine.price(7.0, CarType.HATCHBACK, null);
        assertEquals(0, hatchback.minimumFareAdjustment());
        assertEquals(5400, hatchback.total());
    }

    @Test
    void fractionalDistanceIsProRatedWithinATier() {
        // 2.5km hatchback: 2km at tier 1 (2000) + 0.5km at tier 2 (400) = 2400.
        FareBreakdown breakdown = pricingEngine.price(2.5, CarType.HATCHBACK, null);
        assertEquals(2400, breakdown.tieredFare());
    }

    @Test
    void shortRideHitsTheMinimumFare() {
        // 1km hatchback: tiered fare is 1000, but the 5000 minimum fare wins.
        FareBreakdown breakdown = pricingEngine.price(1.0, CarType.HATCHBACK, null);
        assertEquals(1000, breakdown.tieredFare());
        assertEquals(4000, breakdown.minimumFareAdjustment());
        assertEquals(5000, breakdown.total());
    }

    @Test
    void sameDistanceCostsMoreInSedanThanHatchback() {
        FareBreakdown hatchback = pricingEngine.price(2.0, CarType.HATCHBACK, null);
        FareBreakdown sedan = pricingEngine.price(2.0, CarType.SEDAN, null);
        assertTrue(sedan.tieredFare() > hatchback.tieredFare());
    }

    @Test
    void percentageCouponDiscountsTheFare() {
        FareBreakdown breakdown = pricingEngine.price(7.0, CarType.HATCHBACK, new PercentageDiscount(10, 100_000));
        assertEquals(5400, breakdown.tieredFare());
        assertEquals(540, breakdown.discount());
        assertEquals(4860, breakdown.total());
    }

    @Test
    void percentageCouponIsCappedAtItsMax() {
        // 50% of 5400 would be 2700, but the cap is 1000.
        FareBreakdown breakdown = pricingEngine.price(7.0, CarType.HATCHBACK, new PercentageDiscount(50, 1000));
        assertEquals(1000, breakdown.discount());
        assertEquals(4400, breakdown.total());
    }

    @Test
    void couponLargerThanFareFloorsTotalAtZero() {
        // 150% of the 5000 fare is 7500 - more than the fare itself. Total floors at 0
        // instead of going negative; the discount field still shows the full computed
        // amount so it's clear why total hit the floor.
        FareBreakdown breakdown = pricingEngine.price(1.0, CarType.HATCHBACK, new PercentageDiscount(150, 100_000));
        assertEquals(7500, breakdown.discount());
        assertEquals(0, breakdown.total());
    }

    @Test
    void couponIsAppliedAfterTheMinimumFareFloorNotBeforeIt() {
        // Tiered fare for 1km hatchback is 1000, floored up to the 5000 minimum. A 10%
        // coupon should discount 10% of 5000 (500), not 10% of the raw 1000 (100).
        FareBreakdown breakdown = pricingEngine.price(1.0, CarType.HATCHBACK, new PercentageDiscount(10, 100_000));
        assertEquals(500, breakdown.discount());
        assertEquals(4500, breakdown.total());
    }

    @Test
    void aRideRequestedAsHatchbackIsPricedAtHatchbackRatesEvenWhenAssignedASedan() {
        // PricingEngine only ever sees a CarType, not a Ride - the call site (RideService)
        // is what must pass getRequestedCarType() rather than getAssignedCarType(). This
        // builds a Ride with the two deliberately different, prices it exactly the way
        // RideService.endRide does, and proves the result matches a pure HATCHBACK price
        // (not the SEDAN price for the same distance) - decision #5, tested at the pricing
        // level rather than only through the RideService integration test.
        Ride ride = new Ride("r1", "u1", "d1", CarType.HATCHBACK, CarType.SEDAN,
                new Location(0, 0), new Location(7, 0));
        double distanceKm = ride.getPickupLocation().distanceTo(ride.getDropLocation());

        FareBreakdown actual = pricingEngine.price(distanceKm, ride.getRequestedCarType(), ride.getAppliedDiscount());
        FareBreakdown pureHatchback = pricingEngine.price(7.0, CarType.HATCHBACK, null);
        FareBreakdown pureSedan = pricingEngine.price(7.0, CarType.SEDAN, null);

        assertEquals(CarType.HATCHBACK, actual.pricedAsCarType());
        assertEquals(pureHatchback.total(), actual.total());
        assertNotEquals(pureSedan.total(), actual.total());
    }

    @Test
    void surgeAdjustmentRoundsHalfUpUsingLongArithmeticOnly() {
        // 1km hatchback hits its minimum fare exactly: tieredFare 1000 + adjustment
        // 4000 = 5000. A surge of 10001 bps (1.0001x) makes the surged amount exactly
        // 5000.5 paise - that rounds half-up to 5001, not down to 5000.
        FareBreakdown breakdown = pricingEngine.price(1.0, CarType.HATCHBACK, 10_001, null);
        assertEquals(5000, breakdown.tieredFare() + breakdown.minimumFareAdjustment()); // sanity: base is 5000
        assertEquals(1, breakdown.surgeAdjustment());
        assertEquals(5001, breakdown.total());
    }

    @Test
    void surgeIsAppliedBeforeTheCouponDiscountNotAfter() {
        // 7km hatchback: afterMinimumFloor = 5400. At 1.25x surge, that's 6750. A 10%
        // coupon capped at 600 binds against that POST-surge amount (10% of 6750 = 675
        // > 600) but would NOT bind against the pre-surge amount (10% of 5400 = 540 <
        // 600) - so which order ran is directly observable in the discount and total.
        FareBreakdown breakdown = pricingEngine.price(7.0, CarType.HATCHBACK, 12_500, new PercentageDiscount(10, 600));
        assertEquals(1350, breakdown.surgeAdjustment()); // 5400 * 0.25
        assertEquals(600, breakdown.discount()); // capped - would be 540 if the coupon ran first
        assertEquals(6150, breakdown.total());
    }
}
