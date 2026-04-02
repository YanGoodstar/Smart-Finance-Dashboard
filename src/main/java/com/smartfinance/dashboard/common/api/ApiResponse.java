package com.smartfinance.dashboard.common.api;

public record ApiResponse<T>(boolean success, String code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, ErrorCode.OK.name(), "success", data);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.name(), message, null);
    }
}
