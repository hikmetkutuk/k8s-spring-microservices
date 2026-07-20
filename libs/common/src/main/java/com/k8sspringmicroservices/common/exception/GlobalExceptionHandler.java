package com.k8sspringmicroservices.common.exception;

import com.k8sspringmicroservices.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ErrorResponse> handleApplicationException(
      ApplicationException ex, HttpServletRequest request) {
    log.warn("Application exception: {}", ex.getMessage());
    return buildResponse(ex.getStatus(), ex.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<ErrorResponse.FieldError> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream().map(this::toFieldError).toList();

    ErrorResponse body =
        ErrorResponse.ofValidation(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "Validation failed",
            request.getRequestURI(),
            fieldErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(
      Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception", ex);
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
  }

  private ResponseEntity<ErrorResponse> buildResponse(
      HttpStatus status, String message, HttpServletRequest request) {
    ErrorResponse body =
        ErrorResponse.of(
            status.value(), status.getReasonPhrase(), message, request.getRequestURI());
    return ResponseEntity.status(status).body(body);
  }

  private ErrorResponse.FieldError toFieldError(FieldError fieldError) {
    return new ErrorResponse.FieldError(fieldError.getField(), fieldError.getDefaultMessage());
  }
}
