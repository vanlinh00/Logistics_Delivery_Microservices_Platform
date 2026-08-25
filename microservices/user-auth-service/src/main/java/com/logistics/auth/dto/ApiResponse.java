package com.logistics.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.logistics.auth.constant.MessageCode;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Standard Generic API Response Wrapper for Auth Service.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private List<String> details;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(MessageCode.SUCCESS.getCode())
                .message(message)
                .data(data)
                .details(Collections.emptyList())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, MessageCode messageCode, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(messageCode.getCode())
                .message(message)
                .data(data)
                .details(Collections.emptyList())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "Success!");
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(MessageCode.CREATED.getCode())
                .message(message)
                .data(data)
                .details(Collections.emptyList())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(MessageCode messageCode, String message, List<String> details) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(messageCode.getCode())
                .message(message)
                .data(null)
                .details(details != null ? details : Collections.emptyList())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(MessageCode messageCode, String message) {
        return error(messageCode, message, Collections.emptyList());
    }

    public static <T> ApiResponse<T> error(String code, String message, List<String> details) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(null)
                .details(details != null ? details : Collections.emptyList())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return error(code, message, Collections.emptyList());
    }
}
