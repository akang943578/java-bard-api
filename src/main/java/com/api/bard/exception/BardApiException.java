package com.api.bard.exception;

public class BardApiException extends RuntimeException{

    public BardApiException(String message) {
        super(message);
    }

    public BardApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
