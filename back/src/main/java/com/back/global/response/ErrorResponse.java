package com.back.global.response;

import com.back.global.exception.ErrorCode;

import java.util.List;

public record ErrorResponse(
    String code,
    String message,
    List<FieldError> errors
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), fieldErrors);
    }

    public record FieldError(String field, String message) {}
}
