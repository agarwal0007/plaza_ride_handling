# Ride Hailing Backend

An in-memory ride-hailing backend (Java 17, Maven, JUnit 5, no frameworks). Built in
phases; see git log for the order.

## How to run

```bash
mvn test                              # run the test suite
mvn -q compile exec:java              # start the interactive CLI (reads stdin)
mvn -q compile exec:java < demo.txt   # run the scripted demo (see below)
```

Once running, type `help` for the full command list, or `exit` to quit. Blank lines
and lines starting with `#` are ignored, which is what lets `demo.txt` be both a
runnable script and a commented walkthrough at the same time.

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

**From Phase 5 (CLI):**
- `add_coupon` only accepts the literal type `PERCENT`. The discount design supports
  other types being added later (decision #7), but none beyond percentage was asked
  for, so none beyond `PercentageDiscount` was built.
- The REPL's single catch block around a command's execution catches any
  `RuntimeException`, not just `DomainException`. That one broader catch is what makes
  "never crash on bad input" and "print `ERROR: <message>` for a domain violation"
  the same mechanism - a malformed number or a missing argument fails the exact same
  way a real rule violation does, instead of needing hand-written validation repeated
  in every command.
- The matching radius (5 units) is fixed once, in `Main`, when everything is wired
  together. There's no command to change it at runtime, since none was specified.

**From Phase 10 (highest-rated matching):**
- A driver's rating defaults to 5.0 when `add_driver` omits it - the same way the
  booking radius has one fixed default - rather than requiring every registration to
  specify one.
- The valid range (1.0-5.0) is inclusive on both ends, and is enforced in `Driver`'s
  constructor itself rather than in `DriverService`: an invalid rating can't produce a
  `Driver` object at all, the same fail-fast pattern `Ride.complete()` already uses
  for its own status invariant.
- An unrecognised `-Dmatching` value is treated as a startup configuration error, not
  a runtime one: it's reported and the process exits before the REPL starts, rather
  than silently falling back to `nearest`.

## Design & trade-offs

**Where the seams are, and why.** Three interfaces exist specifically so a whole
category of future change is a new class, not a rewrite:
- `PricingEngine` (concrete, but driven entirely by `RateCardProvider` data) is the
  only thing that knows how a fare is computed. `RideService` calls it with a distance
  and a car type and gets a `FareBreakdown` back - it has no idea what a "tier" or a
  "minimum fare" is.
- `DriverMatchingStrategy` is the only thing that knows how a driver gets picked.
  `RideService` asks it for candidates and claims the first one that's still
  available - it has no idea what "nearest" means, or that there even is a distance
  calculation involved. Two implementations exist (`NearestDriverStrategy`,
  `HighestRatedStrategy`), and `Main` picks between them at startup from
  `-Dmatching=nearest|rated` (default `nearest`). Switching which one is active - or
  adding a third - only ever touches `Main`; `RideService` is passed whichever
  `DriverMatchingStrategy` it gets and has no idea a choice was even made.
- `DiscountStrategy` is the only thing that knows how a coupon's discount is computed.
  Everything else - `Coupon`, `PricingEngine`, `Ride` - just holds or calls one.

The point of all three is the same: `RideService` is a *coordinator*. It decides
*when* to price a ride, match a driver, or apply a discount, and in what order, but
never *how*. That split is what makes the "what I'd do with more time" list below
mostly additive - a new strategy or a new rate, not a change to `RideService` itself.

**Car types, and the upgrade path between them, are data - not RideService logic.**
`CarType.upgradeTo()` returns the next car type to try (HATCHBACK -> SEDAN; SEDAN ->
nothing). `RideService.book` just walks that chain - requested type, then
`upgradeTo()`, then `upgradeTo()` again, until a driver is claimed or the chain runs
out - without ever naming HATCHBACK or SEDAN itself. **Adding a new car type is a
two-file change: a new `CarType` constant (with its `upgradeTo()`, if it should fall
back to something) and a new entry in `RateCardProvider`.** `RideService`,
`DriverMatchingStrategy`, and the CLI don't change at all.

**Why Euclidean distance.** It's the simplest honest distance between two grid points,
and the spec frames locations as `(x, y)` coordinates, not addresses on a road
network. A real product would need actual road distance (see below), but that's a
different, heavier problem (routing, not geometry) and nothing in this exercise asked
for it.

**Why paise, as a `long`, everywhere except the CLI.** Floating-point money is a
classic, well-known source of silent drift - `0.1 + 0.2 != 0.3` in a double, and that
kind of error compounding across tiered-fare math, discounts, and running totals is
exactly the sort of bug that's invisible until an audit. Integers don't have that
problem. The one place a fractional rupee amount is unavoidable is the tiered-fare
math itself, where a fractional km has to multiply an integer rate - `PricingEngine`
rounds that product to the nearest paisa *per tier*, immediately, rather than carrying
a fractional value through further arithmetic. Everywhere else, money is exact integer
addition and subtraction. The CLI's `Money.format` is the only line of code in the
whole project that turns paise into a decimal string, and that's deliberate: it's a
display concern, not a domain one.

**Why a coupon is validated at booking and locked onto the ride, not re-validated at
the end.** The alternative - checking the coupon code again when the ride ends - would
mean a ride's price could change depending on something that happened *during* the
ride (someone deleting the coupon), which is a strange thing for a fare to depend on.
Locking the actual `DiscountStrategy` object onto the `Ride` at booking time means the
ride's price is fully determined the moment it's booked, same as the car type and the
pickup/drop points - deleting the coupon later is bookkeeping, not a price change.

## What I'd do with more time

- **Surge pricing**, as an extra multiplier step in `PricingEngine` between the
  tiered-fare calculation and the minimum-fare floor - it composes with the existing
  "tiered fare -> floor -> discount" pipeline rather than replacing any of it.
- **`HighestRatedStrategy`**, a second `DriverMatchingStrategy` that ranks candidates
  by driver rating instead of distance - this would need a rating field on `Driver`
  that doesn't exist yet.
- **Cancellation fees** - a ride state between "booked" and "ongoing" that can be
  cancelled for a small charge, instead of today's model where a ride is ongoing from
  the moment a driver is claimed.
- **Haversine distance** instead of Euclidean, once locations are real latitude and
  longitude rather than an abstract grid - `Location.distanceTo` is the only method
  that would need to change.
- **A spatial index** (e.g. a grid or a k-d tree) instead of `NearestDriverStrategy`
  scanning every driver on every booking. Fine at this scale; wouldn't be at
  city-wide driver counts.
- **Persistence** - swap the `InMemory*Repository` implementations for ones backed by
  a real database. Every service depends on the repository *interfaces*, never the
  in-memory classes directly, so this is the one item here that's already fully
  prepared for by the existing seams rather than needing a new one.

## How I used AI

TODO
