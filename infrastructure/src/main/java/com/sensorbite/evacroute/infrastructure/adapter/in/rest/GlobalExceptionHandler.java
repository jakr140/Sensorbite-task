package com.sensorbite.evacroute.infrastructure.adapter.in.rest;

import com.sensorbite.evacroute.application.dto.ErrorResponse;
import com.sensorbite.evacroute.application.dto.InternalError;
import com.sensorbite.evacroute.application.dto.NotFoundError;
import com.sensorbite.evacroute.application.dto.ServiceUnavailableError;
import com.sensorbite.evacroute.application.dto.ValidationError;
import com.sensorbite.evacroute.domain.exception.InvalidCoordinateException;
import com.sensorbite.evacroute.domain.exception.RouteNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCoordinateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidCoordinate(InvalidCoordinateException ex) {
        String requestId = UUID.randomUUID().toString();
        log.warn("Invalid coordinate [requestId={}]: {}", requestId, ex.getMessage());

        return new ValidationError(
                "VALIDATION_ERROR",
                ex.getMessage(),
                Instant.now().toString(),
                requestId
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        String requestId = UUID.randomUUID().toString();
        log.warn("Invalid request [requestId={}]: {}", requestId, ex.getMessage());

        return new ValidationError(
                "VALIDATION_ERROR",
                ex.getMessage(),
                Instant.now().toString(),
                requestId
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(ConstraintViolationException ex) {
        String requestId = UUID.randomUUID().toString();
        String errors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation [requestId={}]: {}", requestId, errors);

        return new ValidationError(
                "VALIDATION_ERROR",
                errors,
                Instant.now().toString(),
                requestId
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String requestId = UUID.randomUUID().toString();
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed [requestId={}]: {}", requestId, errors);

        return new ValidationError(
                "VALIDATION_ERROR",
                errors,
                Instant.now().toString(),
                requestId
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParameter(MissingServletRequestParameterException ex) {
        String requestId = UUID.randomUUID().toString();
        log.warn("Missing parameter [requestId={}]: {}", requestId, ex.getMessage());

        return new ValidationError(
                "VALIDATION_ERROR",
                ex.getParameterName() + " parameter is required",
                Instant.now().toString(),
                requestId
        );
    }

    @ExceptionHandler(RouteNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleRouteNotFound(RouteNotFoundException ex) {
        String requestId = UUID.randomUUID().toString();
        log.warn("Route not found [requestId={}]: {}", requestId, ex.getMessage());

        return new NotFoundError(
                "ROUTE_NOT_FOUND",
                ex.getMessage(),
                Instant.now().toString(),
                requestId
        );
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleTimeout(AsyncRequestTimeoutException ex) {
        String requestId = UUID.randomUUID().toString();
        log.warn("Route computation timeout [requestId={}]", requestId);

        return new ServiceUnavailableError(
                "SERVICE_UNAVAILABLE",
                "Route computation timeout (exceeded 30 seconds)",
                Instant.now().toString(),
                requestId
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception ex) {
        String requestId = UUID.randomUUID().toString();
        log.error("Unexpected error [requestId={}]: {}", requestId, ex.getMessage(), ex);

        return new InternalError(
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please contact support with request ID: " + requestId,
                Instant.now().toString(),
                requestId
        );
    }
}
