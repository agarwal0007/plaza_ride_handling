# Ride Hailing Backend

An in-memory ride-hailing backend (Java 17, Maven, JUnit 5, no frameworks). Built in
phases; see git log for the order.

## How to run

(Added in Phase 5, once the CLI exists.)

## Assumptions

These are the design decisions handed down for this exercise, restated in plain English
so it's clear what the code is doing and why, without re-deriving it from the diff:

1. **Money is integer paise, not a decimal.** `long` holds ₹ amounts as paise (₹1 = 100
   paise) everywhere in the domain and pricing code. Floats/doubles are never used for
   money, so rounding never silently drifts. The CLI is the only place that formats a
   paise value back into ₹x.yy for display.
2. **Distance is straight-line, not road distance.** Two points are `(x, y)` on a grid;
   the distance between them is plain Euclidean distance, reported in km as a `double`.
3. **Fares are tiered like income-tax slabs, not flat-rate.** Each car type has ordered
   distance bands (e.g. 0-2 km, 2-5 km, beyond 5 km) with its own per-km rate; a ride
   pays each band's rate only for the portion of the distance that falls in that band,
   and fractional km are pro-rated. Every car type also has its own minimum fare. Adding
   a new car type or changing a rate is a data change in one place, not new code.
4. **Fare calculation happens in a fixed order:** compute the tiered fare, raise it to
   the minimum fare if it's below that floor, then apply the coupon discount, then floor
   the result at 0. Because the discount is applied *after* the minimum-fare floor, a big
   enough coupon can take a ride below its minimum fare - the platform eats that
   difference, the rider doesn't lose the discount.
5. **A ride remembers both what was requested and what was assigned**, and always prices
   using what was *requested*. This is the only reason the free-upgrade rule (below)
   works: if a HATCHBACK was asked for but a SEDAN was assigned, the rider still pays
   HATCHBACK rates.
6. **A coupon is checked once, at booking, and then locked in.** If the code is invalid
   or unknown, booking fails outright - it never just gets silently skipped. Once booked,
   the ride keeps its own reference to the discount logic (not just the coupon's code),
   so deleting that coupon later has no effect on a ride that already locked it in.
7. **A coupon is a code plus a pluggable discount calculation.** The first discount type
   is "percent off, capped at a maximum rupee amount." Discounts are written so that a
   different kind (e.g. a flat rupees-off coupon) can be added as one new class, without
   touching the coupon or pricing code.
8. **A user or a driver can only be mid-ride once.** Trying to book a second ride while
   one is still ongoing is rejected, for both the rider and the driver.
9. **A driver is either AVAILABLE or ON_TRIP**, nothing in between. Ending a ride flips
   the driver back to AVAILABLE and moves their cab's location to the ride's drop point -
   that's where they actually are now.
10. **Finding a driver and deciding to fall back to a different car type are two separate
    concerns.** The matching logic only knows "find the nearest AVAILABLE driver of this
    exact type within the radius." The HATCHBACK-unavailable-so-offer-SEDAN rule is a
    pricing/business decision layered on top of that, not part of matching itself.
11. **Claiming a driver has to be race-safe.** Two riders booking at the same instant
    must not both end up with the same driver. A driver's status is updated with an
    atomic compare-and-swap (AVAILABLE -> ON_TRIP); if that fails because someone else
    just claimed them, the system moves on to the next-nearest candidate instead of
    giving up immediately.
12. **A ride can only be ended once.** Ending an already-completed ride is rejected
    rather than silently doing nothing or re-computing a new fare.

(More assumptions - things not covered by the twelve decisions above - get added here as
they come up in later phases.)

**From Phase 3 (services):**
- Re-registering a user or driver under an id that's already taken simply overwrites
  the old record - there's no "id already exists" error.
- Deleting a coupon code that doesn't exist is a no-op, not an error.
- If booking fails for any reason (no driver found, invalid coupon), no `Ride` is ever
  created - it's as if the booking never happened, which is also why a failed booking
  attempt never shows up in anyone's ride history.

## Design & trade-offs

(Added in Phase 6.)

## What I'd do with more time

(Added in Phase 6.)

## How I used AI

TODO
