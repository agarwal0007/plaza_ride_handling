package com.ridehailing.cli;

import com.ridehailing.matching.NearestDriverStrategy;
import com.ridehailing.pricing.PricingEngine;
import com.ridehailing.pricing.RateCardProvider;
import com.ridehailing.repo.CouponRepository;
import com.ridehailing.repo.DriverRepository;
import com.ridehailing.repo.InMemoryCouponRepository;
import com.ridehailing.repo.InMemoryDriverRepository;
import com.ridehailing.repo.InMemoryRideRepository;
import com.ridehailing.repo.InMemoryUserRepository;
import com.ridehailing.repo.RideRepository;
import com.ridehailing.repo.UserRepository;
import com.ridehailing.service.CouponService;
import com.ridehailing.service.DriverService;
import com.ridehailing.service.RideService;
import com.ridehailing.service.UserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Composition root: this is the only place that constructs repositories, services,
 * and strategies and wires them together. Nothing else in the codebase knows about
 * these concrete classes at once.
 */
public class Main {

    private static final double DEFAULT_RADIUS_KM = 5.0;

    public static void main(String[] args) throws IOException {
        UserRepository userRepository = new InMemoryUserRepository();
        DriverRepository driverRepository = new InMemoryDriverRepository();
        RideRepository rideRepository = new InMemoryRideRepository();
        CouponRepository couponRepository = new InMemoryCouponRepository();

        UserService userService = new UserService(userRepository);
        DriverService driverService = new DriverService(driverRepository);
        CouponService couponService = new CouponService(couponRepository);
        PricingEngine pricingEngine = new PricingEngine(new RateCardProvider());
        NearestDriverStrategy matchingStrategy = new NearestDriverStrategy();
        RideService rideService = new RideService(userRepository, driverRepository, rideRepository,
                couponService, pricingEngine, matchingStrategy, DEFAULT_RADIUS_KM);

        CommandRegistry registry = new CommandRegistry();
        registry.register("add_user", new AddUserCommand(userService));
        registry.register("add_driver", new AddDriverCommand(driverService));
        registry.register("update_location", new UpdateLocationCommand(driverService));
        registry.register("add_coupon", new AddCouponCommand(couponService));
        registry.register("delete_coupon", new DeleteCouponCommand(couponService));
        registry.register("book_ride", new BookRideCommand(rideService));
        registry.register("end_ride", new EndRideCommand(rideService));
        registry.register("user_history", new UserHistoryCommand(rideService));
        registry.register("driver_history", new DriverHistoryCommand(rideService));
        registry.register("help", new HelpCommand());
        registry.register("exit", new ExitCommand());

        runRepl(registry);
    }

    private static void runRepl(CommandRegistry registry) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\s+");
            String commandName = parts[0];
            String[] commandArgs = Arrays.copyOfRange(parts, 1, parts.length);

            Command command = registry.find(commandName);
            if (command == null) {
                System.out.println("ERROR: Unknown command: " + commandName + " (type 'help' for a list)");
                continue;
            }

            try {
                command.execute(commandArgs);
            } catch (RuntimeException e) {
                // Catches DomainException (a real rule violation) and also things like
                // NumberFormatException or ArrayIndexOutOfBoundsException from malformed
                // input - either way, bad input prints an error instead of crashing the REPL.
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }
}
