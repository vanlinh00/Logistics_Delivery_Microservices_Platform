package com.logistics.fulfillment.exception;

import com.logistics.fulfillment.constant.MessageCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private final MessageCode messageCode;

    public ResourceNotFoundException(String message) {
        super(message);
        this.messageCode = MessageCode.NOT_FOUND;
    }

    public ResourceNotFoundException(MessageCode messageCode, String message) {
        super(message);
        this.messageCode = messageCode;
    }
}
