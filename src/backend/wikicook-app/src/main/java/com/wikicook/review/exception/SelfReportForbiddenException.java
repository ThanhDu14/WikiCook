package com.wikicook.review.exception;

public class SelfReportForbiddenException extends RuntimeException {
    public SelfReportForbiddenException(String message) {
        super(message);
    }
}

