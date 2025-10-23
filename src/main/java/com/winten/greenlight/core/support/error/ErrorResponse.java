package com.winten.greenlight.core.support.error;

import org.springframework.http.HttpStatus;

public record ErrorResponse(HttpStatus status, ErrorCode errorCode, String message, Object detail) {
    public ErrorResponse(CoreException exception) {
        this(exception.getErrorType().getStatus(),
                exception.getErrorType().getCode(),
                exception.getErrorType().getMessage(),
                exception.getDetail()
        );
    }
}