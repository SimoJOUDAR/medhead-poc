package com.medhead.poc.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.medhead.poc.application.dto.ErrorResponse;
import com.medhead.poc.domain.exception.HospitalNotFoundException;
import com.medhead.poc.domain.exception.NoBedAvailableException;
import com.medhead.poc.domain.exception.OptimisticLockConflictException;
import com.medhead.poc.domain.exception.SpecialtyNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNoBedAvailable_mapsTo404WithCode() {
        ResponseEntity<ErrorResponse> response = handler.handleNoBedAvailable(new NoBedAvailableException(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(404);
        assertThat(body.code()).isEqualTo("NO_BEDS_AVAILABLE");
        assertThat(body.message()).contains("42");
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.details()).isNull();
    }

    @Test
    void handleHospitalNotFound_mapsTo404WithCode() {
        ResponseEntity<ErrorResponse> response = handler.handleHospitalNotFound(new HospitalNotFoundException(9999L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("HOSPITAL_NOT_FOUND");
        assertThat(response.getBody().message()).contains("9999");
    }

    @Test
    void handleSpecialtyNotFound_mapsTo404WithCode() {
        ResponseEntity<ErrorResponse> response = handler.handleSpecialtyNotFound(new SpecialtyNotFoundException(123L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("SPECIALTY_NOT_FOUND");
        assertThat(response.getBody().message()).contains("123");
    }

    @Test
    void handleMethodArgumentNotValid_mapsTo400WithFieldDetails() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "recommendationRequest");
        bindingResult.addError(new FieldError("recommendationRequest", "latitude", "must be less than or equal to 90.0"));
        bindingResult.addError(new FieldError("recommendationRequest", "specialtyId", "must not be null"));
        MethodParameter methodParameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class), 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValid(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.details())
                .extracting(ErrorResponse.FieldError::field)
                .containsExactlyInAnyOrder("latitude", "specialtyId");
        assertThat(body.details())
                .extracting(ErrorResponse.FieldError::message)
                .contains("must be less than or equal to 90.0", "must not be null");
    }

    @Test
    void handleConstraintViolation_mapsTo400WithFieldDetails() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        when(path.toString()).thenReturn("listHospitals.id");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be positive");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.details()).hasSize(1);
        assertThat(body.details().get(0).field()).isEqualTo("listHospitals.id");
        assertThat(body.details().get(0).message()).isEqualTo("must be positive");
    }

    @Test
    void handleHttpMessageNotReadable_mapsTo400() {
        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("malformed", (org.springframework.http.HttpInputMessage) null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("MALFORMED_REQUEST");
        assertThat(response.getBody().details()).isNull();
    }

    @Test
    void handleOptimisticLock_mapsTo503() {
        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLock(new OptimisticLockConflictException(77L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESERVATION_CONFLICT");
    }

    @Test
    void handleUnexpected_mapsTo500WithoutLeakingInternals() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new RuntimeException("NullPointerException at com.internal.Foo:42"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.message()).doesNotContain("NullPointerException");
        assertThat(body.message()).doesNotContain("com.internal");
    }

    @Test
    void handleBadCredentials_keepsLegacyShape() {
        ResponseEntity<?> response = handler.handleBadCredentials(new BadCredentialsException("bad"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(java.util.Map.class);
        @SuppressWarnings("unchecked")
        var body = (java.util.Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "invalid_credentials");
    }

    @SuppressWarnings("unused")
    private void dummy(String value) {
    }
}
