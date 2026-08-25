package com.logistics.auth.constant;

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
    GROUP_NOT_FOUND("i.xx.fw.405"),
    USER_NOT_FOUND("i.xx.fw.406"),
    ACCOUNT_INACTIVE("i.xx.fw.407"),
    USER_ALREADY_EXISTS("i.xx.fw.408"),
    CONFLICT("i.xx.fw.409"),
    TOKEN_INVALID("i.xx.fw.410"),
    VALIDATION_FAILED("i.xx.fw.411"),
    PASSWORD_MISMATCH("i.xx.fw.412"),
    MFA_INVALID("i.xx.fw.413"),
    MFA_REQUIRED("i.xx.fw.414"),
    ROLE_NOT_FOUND("i.xx.fw.415"),
    COURIER_PROFILE_NOT_FOUND("i.xx.fw.416"),
    MERCHANT_PROFILE_NOT_FOUND("i.xx.fw.417"),

    // 5xx Server Error Codes
    INTERNAL_SERVER_ERROR("i.xx.fw.500");

    private final String code;
}
