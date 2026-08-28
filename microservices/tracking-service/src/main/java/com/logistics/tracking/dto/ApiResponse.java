package com.logistics.tracking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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
                .code(com.logistics.tracking.constant.MessageCode.SUCCESS.getCode())
                .message(message)
                .data(data)
                .details(Collections.emptyList())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "Operation completed successfully");
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(com.logistics.tracking.constant.MessageCode.CREATED.getCode())
                .message(message)
                .data(data)
                .details(Collections.emptyList())
                .timestamp(LocalDateTime.now())
                .build();
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

    public static <T> ApiResponse<T> error(com.logistics.tracking.constant.MessageCode messageCode, String message) {
        return error(messageCode.getCode(), message, Collections.emptyList());
    }
}
