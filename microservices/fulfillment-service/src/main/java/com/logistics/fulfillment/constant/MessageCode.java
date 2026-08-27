package com.logistics.fulfillment.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Standardized Application Message and Error Code Enum for Fulfillment Service.
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
    POD_NOT_FOUND("i.xx.fw.405.pod"),
    TRANSIT_NOT_FOUND("i.xx.fw.405.transit"),
    CONFLICT("i.xx.fw.409"),
    TOKEN_INVALID("i.xx.fw.410"),
    VALIDATION_FAILED("i.xx.fw.411"),
    CONSTRAINT_VIOLATION("i.xx.fw.412"),
    BUSINESS_RULE_VIOLATION("i.xx.fw.413"),

    // 5xx Server Error Codes
    INTERNAL_SERVER_ERROR("i.xx.fw.500");

    private final String code;
}
