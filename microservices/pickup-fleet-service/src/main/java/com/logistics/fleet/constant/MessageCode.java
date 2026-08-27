package com.logistics.fleet.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Standardized Application Message and Error Code Enum.
 */
@Getter
@RequiredArgsConstructor
public enum MessageCode {

    // 2xx Success Codes
    SUCCESS("i.xx.fw.200"),
    CREATED("i.xx.fw.201"),

    // 4xx Client Error Codes
    BAD_REQUEST("i.xx.fw.400"),
    UNAUTHORIZED("i.xx.fw.401"),
    FORBIDDEN("i.xx.fw.403"),
    NOT_FOUND("i.xx.fw.404"),
    DRIVER_NOT_FOUND("i.xx.fw.405.driver"),
    TASK_NOT_FOUND("i.xx.fw.405.task"),
    DRIVER_UNAVAILABLE("i.xx.fw.409.driver"),
    CONFLICT("i.xx.fw.409"),
    TOKEN_INVALID("i.xx.fw.410"),
    VALIDATION_FAILED("i.xx.fw.411"),
    CONSTRAINT_VIOLATION("i.xx.fw.412"),

    // 5xx Server Error Codes
    INTERNAL_SERVER_ERROR("i.xx.fw.500");

    private final String code;
}
