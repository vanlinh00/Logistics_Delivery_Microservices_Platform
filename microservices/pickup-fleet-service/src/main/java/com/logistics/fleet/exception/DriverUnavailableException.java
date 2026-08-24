package com.logistics.fleet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DriverUnavailableException extends RuntimeException {
    public DriverUnavailableException(String message) {
        super(message);
    }
}
