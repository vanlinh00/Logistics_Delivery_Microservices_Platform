package com.logistics.auth.exception;

import com.logistics.auth.constant.MessageCode;
import com.logistics.auth.dto.ApiResponse;
import com.logistics.auth.service.MessageService;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * Global Exception Handler for User Auth Service using standardized MessageCodes.
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageService messageService;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        String msg = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : messageService.getMessage(MessageCode.NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(MessageCode.NOT_FOUND, msg));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        String msg = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : messageService.getMessage(MessageCode.UNAUTHORIZED);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(MessageCode.UNAUTHORIZED, msg));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(MessageCode.UNAUTHORIZED, messageService.getMessage(MessageCode.UNAUTHORIZED)));
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountInactive(AccountInactiveException ex) {
        log.warn("Account inactive: {}", ex.getMessage());
        String msg = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : messageService.getMessage(MessageCode.ACCOUNT_INACTIVE);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(MessageCode.ACCOUNT_INACTIVE, msg));
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateUser(DuplicateUserException ex) {
        log.warn("Duplicate user error: {}", ex.getMessage());
        String msg = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : messageService.getMessage(MessageCode.USER_ALREADY_EXISTS);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(MessageCode.USER_ALREADY_EXISTS, msg));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(MessageCode.FORBIDDEN, messageService.getMessage(MessageCode.FORBIDDEN)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> details = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.add(String.format("Field '%s': %s", error.getField(), error.getDefaultMessage()));
        }
        log.warn("DTO validation failed: {}", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(MessageCode.VALIDATION_FAILED, messageService.getMessage(MessageCode.VALIDATION_FAILED), details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(cv -> String.format("%s: %s", cv.getPropertyPath(), cv.getMessage()))
                .toList();
        log.warn("Constraint violation: {}", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(MessageCode.VALIDATION_FAILED, messageService.getMessage(MessageCode.VALIDATION_FAILED), details));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(MessageCode.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(MessageCode.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Unhandled error in user-auth-service: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(MessageCode.INTERNAL_SERVER_ERROR, messageService.getMessage(MessageCode.INTERNAL_SERVER_ERROR)));
    }
}
