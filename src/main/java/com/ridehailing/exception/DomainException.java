package com.ridehailing.exception;

/**
 * Base type for all domain-rule violations (e.g. no driver available, double booking,
 * invalid coupon). Unchecked, so callers aren't forced to catch it at every call site -
 * the CLI loop catches it in one place and prints "ERROR: <message>".
 */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
