package com.lumora.sms.exception;

/** Raised when a requested Lumora resource does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object identifier) { super(resource + " was not found: " + identifier); }
}
