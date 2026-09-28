package com.campusconnect.dto;

/**
 * The single response shape used by every JSON endpoint, as required by the API
 * contract: {@code {success, message, data}}.
 *
 * <p>CSV and file downloads deliberately bypass this envelope, because they are
 * binary attachments rather than JSON.
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Request successful", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
