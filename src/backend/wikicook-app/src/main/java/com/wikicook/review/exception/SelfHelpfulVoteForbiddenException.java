package com.wikicook.review.exception;

public class SelfHelpfulVoteForbiddenException extends RuntimeException {
    public SelfHelpfulVoteForbiddenException(String message) {
        super(message);
    }
}

