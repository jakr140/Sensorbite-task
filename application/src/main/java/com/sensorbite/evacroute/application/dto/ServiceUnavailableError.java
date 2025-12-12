package com.sensorbite.evacroute.application.dto;

public record ServiceUnavailableError(
    String errorType,
    String message,
    String timestamp,
    String requestId
) implements ErrorResponse {
    public ServiceUnavailableError {
        if (errorType == null || message == null || timestamp == null || requestId == null) {
            throw new IllegalArgumentException("Error response fields cannot be null");
        }
    }
}
