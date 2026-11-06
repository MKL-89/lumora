package com.lumora.sms.exception;

/** Raised when a value that must be unique already exists. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String resource, String value) { super(resource + " already exists: " + value); }
}
