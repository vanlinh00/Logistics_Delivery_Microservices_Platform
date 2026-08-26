package com.logistics.order.constant;

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
    ORDER_NOT_FOUND("i.xx.fw.405"),
    INVALID_STATUS_TRANSITION("i.xx.fw.406"),
    BUSINESS_RULE_VIOLATION("i.xx.fw.407"),
    CONFLICT("i.xx.fw.409"),
    TOKEN_INVALID("i.xx.fw.410"),
    VALIDATION_FAILED("i.xx.fw.411"),
    ADDRESS_INVALID("i.xx.fw.418"),
    SENDER_ADDRESS_INVALID("i.xx.fw.418.sender"),
    RECIPIENT_ADDRESS_INVALID("i.xx.fw.418.recipient"),
    ADDRESS_TOO_SHORT("i.xx.fw.418.too_short"),
    PHONE_INVALID("i.xx.fw.418.phone"),
    ADDRESS_VALID("i.xx.fw.418.valid"),
    ORDER_LOCK_FAILED("i.xx.fw.409.lock"),

    // 5xx Server Error Codes
    INTERNAL_SERVER_ERROR("i.xx.fw.500"),
    ORDER_INTERRUPTED("i.xx.fw.500.interrupted");

    private final String code;
}
