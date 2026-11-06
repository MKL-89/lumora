package com.lumora.sms.exception;

/** Raised when an operation would violate an explicit school business rule. */
public class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException(String message) { super(message); }
}
