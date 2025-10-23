package com.winten.greenlight.core.support.error;

import lombok.Getter;

@Getter
public class CoreException extends RuntimeException {

    private final ErrorType errorType;

    private final Object detail; // 상세한 오류 내용

    public CoreException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.detail = null;
    }

    public CoreException(ErrorType errorType, Object detail) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.detail = detail;
    }

    public static CoreException of(ErrorType errorType) {
        return CoreException.of(errorType, null);
    }

    public static CoreException of(ErrorType errorType, Object detail) {
        return new CoreException(errorType, detail);
    }

}