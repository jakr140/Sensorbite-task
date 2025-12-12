package com.sensorbite.evacroute.domain.exception;

public class InvalidCoordinateException extends RuntimeException {
    public InvalidCoordinateException(String message) {
        super(message);
    }
}
