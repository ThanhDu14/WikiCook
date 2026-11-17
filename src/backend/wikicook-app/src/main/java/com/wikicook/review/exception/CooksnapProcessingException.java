package com.wikicook.review.exception;

public class CooksnapProcessingException extends RuntimeException {
    public CooksnapProcessingException(String message) {
        super(message);
    }

    public CooksnapProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

