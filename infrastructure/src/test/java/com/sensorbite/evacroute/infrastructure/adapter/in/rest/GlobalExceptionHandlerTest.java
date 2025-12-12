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
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("InvalidCoordinateException handling")
    class InvalidCoordinateExceptionTests {

        @Test
        @DisplayName("should return validation error for invalid coordinate")
        void shouldReturnValidationErrorForInvalidCoordinate() {
            InvalidCoordinateException ex = new InvalidCoordinateException("Invalid latitude: 91.0");

            ErrorResponse response = handler.handleInvalidCoordinate(ex);

            assertThat(response).isInstanceOf(ValidationError.class);
            ValidationError error = (ValidationError) response;
            assertThat(error.errorType()).isEqualTo("VALIDATION_ERROR");
            assertThat(error.message()).contains("Invalid latitude: 91.0");
            assertThat(error.timestamp()).isNotNull();
            assertThat(error.requestId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException handling")
    class IllegalArgumentExceptionTests {

        @Test
        @DisplayName("should return validation error for illegal argument")
        void shouldReturnValidationErrorForIllegalArgument() {
            IllegalArgumentException ex = new IllegalArgumentException("Distance exceeds maximum");

            ErrorResponse response = handler.handleIllegalArgument(ex);

            assertThat(response).isInstanceOf(ValidationError.class);
            ValidationError error = (ValidationError) response;
            assertThat(error.errorType()).isEqualTo("VALIDATION_ERROR");
            assertThat(error.message()).contains("Distance exceeds maximum");
            assertThat(error.requestId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("ConstraintViolationException handling")
    class ConstraintViolationExceptionTests {

        @Test
        @DisplayName("should return validation error with constraint violations")
        void shouldReturnValidationErrorWithConstraintViolations() {
            ConstraintViolation<?> violation1 = createMockViolation("start", "must not be blank");
            ConstraintViolation<?> violation2 = createMockViolation("end", "must not be blank");
            Set<ConstraintViolation<?>> violations = Set.of(violation1, violation2);
            ConstraintViolationException ex = new ConstraintViolationException(violations);

            ErrorResponse response = handler.handleConstraintViolation(ex);

            assertThat(response).isInstanceOf(ValidationError.class);
            ValidationError error = (ValidationError) response;
            assertThat(error.errorType()).isEqualTo("VALIDATION_ERROR");
            assertThat(error.message()).contains("start");
            assertThat(error.message()).contains("must not be blank");
            assertThat(error.requestId()).isNotNull();
        }

        @SuppressWarnings("unchecked")
        private ConstraintViolation<?> createMockViolation(String propertyPath, String message) {
            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            Path path = mock(Path.class);
            when(path.toString()).thenReturn(propertyPath);
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getMessage()).thenReturn(message);
            return violation;
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException handling")
    class MethodArgumentNotValidExceptionTests {

        @Test
        @DisplayName("should return validation error with field errors")
        void shouldReturnValidationErrorWithFieldErrors() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);

            FieldError error1 = new FieldError("routeRequest", "start", "must not be blank");
            FieldError error2 = new FieldError("routeRequest", "end", "invalid format");

            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(error1, error2));

            ErrorResponse response = handler.handleMethodArgumentNotValid(ex);

            assertThat(response).isInstanceOf(ValidationError.class);
            ValidationError error = (ValidationError) response;
            assertThat(error.errorType()).isEqualTo("VALIDATION_ERROR");
            assertThat(error.message()).contains("start");
            assertThat(error.message()).contains("end");
            assertThat(error.requestId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("RouteNotFoundException handling")
    class RouteNotFoundExceptionTests {

        @Test
        @DisplayName("should return not found error")
        void shouldReturnNotFoundError() {
            RouteNotFoundException ex = new RouteNotFoundException("No route available");

            ErrorResponse response = handler.handleRouteNotFound(ex);

            assertThat(response).isInstanceOf(NotFoundError.class);
            NotFoundError error = (NotFoundError) response;
            assertThat(error.errorType()).isEqualTo("ROUTE_NOT_FOUND");
            assertThat(error.message()).contains("No route available");
            assertThat(error.timestamp()).isNotNull();
            assertThat(error.requestId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("AsyncRequestTimeoutException handling")
    class AsyncRequestTimeoutExceptionTests {

        @Test
        @DisplayName("should return service unavailable error")
        void shouldReturnServiceUnavailableError() {
            AsyncRequestTimeoutException ex = new AsyncRequestTimeoutException();

            ErrorResponse response = handler.handleTimeout(ex);

            assertThat(response).isInstanceOf(ServiceUnavailableError.class);
            ServiceUnavailableError error = (ServiceUnavailableError) response;
            assertThat(error.errorType()).isEqualTo("SERVICE_UNAVAILABLE");
            assertThat(error.message()).contains("timeout");
            assertThat(error.timestamp()).isNotNull();
            assertThat(error.requestId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Generic Exception handling")
    class GenericExceptionTests {

        @Test
        @DisplayName("should return internal error for unexpected exceptions")
        void shouldReturnInternalErrorForUnexpectedExceptions() {
            Exception ex = new RuntimeException("Unexpected error occurred");

            ErrorResponse response = handler.handleUnexpected(ex);

            assertThat(response).isInstanceOf(InternalError.class);
            InternalError error = (InternalError) response;
            assertThat(error.errorType()).isEqualTo("INTERNAL_ERROR");
            assertThat(error.message()).contains("unexpected error occurred");
            assertThat(error.message()).contains("request ID");
            assertThat(error.timestamp()).isNotNull();
            assertThat(error.requestId()).isNotNull();
        }

        @Test
        @DisplayName("should not expose stack trace in error response")
        void shouldNotExposeStackTraceInErrorResponse() {
            Exception ex = new RuntimeException("Database connection failed");

            ErrorResponse response = handler.handleUnexpected(ex);

            InternalError error = (InternalError) response;
            assertThat(error.message()).doesNotContain("at com.sensorbite");
            assertThat(error.message()).doesNotContain("SQLException");
        }
    }

    @Nested
    @DisplayName("Request ID generation")
    class RequestIdGenerationTests {

        @Test
        @DisplayName("should generate unique request IDs")
        void shouldGenerateUniqueRequestIds() {
            InvalidCoordinateException ex1 = new InvalidCoordinateException("Error 1");
            InvalidCoordinateException ex2 = new InvalidCoordinateException("Error 2");

            ValidationError error1 = (ValidationError) handler.handleInvalidCoordinate(ex1);
            ValidationError error2 = (ValidationError) handler.handleInvalidCoordinate(ex2);

            assertThat(error1.requestId()).isNotEqualTo(error2.requestId());
        }

        @Test
        @DisplayName("should include request ID in all error responses")
        void shouldIncludeRequestIdInAllErrorResponses() {
            ErrorResponse[] responses = {
                    handler.handleInvalidCoordinate(new InvalidCoordinateException("test")),
                    handler.handleIllegalArgument(new IllegalArgumentException("test")),
                    handler.handleRouteNotFound(new RouteNotFoundException("test")),
                    handler.handleTimeout(new AsyncRequestTimeoutException()),
                    handler.handleUnexpected(new RuntimeException("test"))
            };

            for (ErrorResponse response : responses) {
                if (response instanceof ValidationError error) {
                    assertThat(error.requestId()).isNotNull().isNotBlank();
                } else if (response instanceof NotFoundError error) {
                    assertThat(error.requestId()).isNotNull().isNotBlank();
                } else if (response instanceof ServiceUnavailableError error) {
                    assertThat(error.requestId()).isNotNull().isNotBlank();
                } else if (response instanceof InternalError error) {
                    assertThat(error.requestId()).isNotNull().isNotBlank();
                }
            }
        }
    }
}
