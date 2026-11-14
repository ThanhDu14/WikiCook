package com.wikicook.review.exception;

public class SelfReviewForbiddenException extends RuntimeException {
    public SelfReviewForbiddenException(String message) {
        super(message);
    }
}

