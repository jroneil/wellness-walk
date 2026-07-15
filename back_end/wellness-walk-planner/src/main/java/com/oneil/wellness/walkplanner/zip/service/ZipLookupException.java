package com.oneil.wellness.walkplanner.zip.service;

public class ZipLookupException extends RuntimeException {

    public ZipLookupException(String message) {
        super(message);
    }

    public ZipLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
