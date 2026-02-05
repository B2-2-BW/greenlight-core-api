package com.winten.greenlight.core.support.error;

import org.springframework.http.HttpStatus;

public record ErrorResponse(HttpStatus status, String message, Object detail) {
    public ErrorResponse(CoreException exception) {
        this(exception.getErrorCode().getStatus(),
                exception.getErrorCode().getMessage(),
                exception.getDetail()
        );
    }
}