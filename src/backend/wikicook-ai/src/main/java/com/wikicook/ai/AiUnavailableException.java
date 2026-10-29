package com.wikicook.ai;

/**
 * The AI provider cannot be used right now: offline mode, timeout, HTTP 429/5xx or a malformed reply.
 * Callers fall back to the non-AI path; the message never contains the API key or the provider's raw error.
 */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
