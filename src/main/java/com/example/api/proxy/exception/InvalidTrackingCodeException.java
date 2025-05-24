package com.example.api.proxy.exception;

public class InvalidTrackingCodeException extends RuntimeException {
    public InvalidTrackingCodeException(String message) {
        super(message);
    }
}
