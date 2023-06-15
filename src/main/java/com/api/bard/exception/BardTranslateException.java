package com.api.bard.exception;

public class BardTranslateException extends RuntimeException {

    public BardTranslateException(String message) {
        super(message);
    }

    public BardTranslateException(String message, Throwable cause) {
        super(message, cause);
    }
}
