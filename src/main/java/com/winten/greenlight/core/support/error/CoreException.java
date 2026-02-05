package com.winten.greenlight.core.support.error;

import lombok.Getter;

@Getter
public class CoreException extends RuntimeException {

    private final ErrorCode errorCode;

    private final Object detail; // 상세한 오류 내용

    public CoreException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    public CoreException(ErrorCode errorCode, Object detail) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public static CoreException of(ErrorCode errorCode) {
        return CoreException.of(errorCode, null);
    }

    public static CoreException of(ErrorCode errorCode, Object detail) {
        return new CoreException(errorCode, detail);
    }

}