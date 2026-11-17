package com.wikicook.review.exception;

public class ReviewUnderModerationException extends RuntimeException {
    public ReviewUnderModerationException(String message) {
        super(message);
    }
}

