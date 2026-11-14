package com.wikicook.review.exception;

public class ReportAlreadyExistsException extends RuntimeException {
    public ReportAlreadyExistsException(String message) {
        super(message);
    }
}

