package com.ridehailing.cli;

/** Formats a paise amount as ₹x.yy - the only place money becomes a display string (decision #1). */
final class Money {
    private Money() {
    }

    static String format(long paise) {
        return String.format("₹%.2f", paise / 100.0);
    }
}
