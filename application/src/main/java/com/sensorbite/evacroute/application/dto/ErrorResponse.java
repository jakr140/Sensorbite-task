package com.sensorbite.evacroute.application.dto;

public sealed interface ErrorResponse permits
    ValidationError,
    NotFoundError,
    InternalError,
    ServiceUnavailableError {

    String errorType();
    String message();
    String timestamp();
    String requestId();
}
