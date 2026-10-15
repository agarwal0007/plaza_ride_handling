package com.ridehailing.cli;

public class HelpCommand implements Command {
    @Override
    public void execute(String[] args) {
        System.out.println("Commands:");
        System.out.println("  add_user <userId> <name>");
        System.out.println("  add_driver <driverId> <name> <SEDAN|HATCHBACK> <plate> <x> <y> [rating]");
        System.out.println("  update_location <driverId> <x> <y>");
        System.out.println("  add_coupon <code> <TYPE> <args...>   (PERCENT <percent> <maxCapRupees>)");
        System.out.println("  delete_coupon <code>");
        System.out.println("  book_ride <rideId> <userId> <SEDAN|HATCHBACK> <px> <py> <dx> <dy> [couponCode]");
        System.out.println("  end_ride <rideId>");
        System.out.println("  user_history <userId>");
        System.out.println("  driver_history <driverId>");
        System.out.println("  help");
        System.out.println("  exit");
    }
}
