package com.myfinance.track.ai;

import java.time.Duration;

public class AiProviderException extends RuntimeException {

    public enum Kind {
        RATE_LIMITED,   // HTTP 429: retry later, or fall back
        UNAVAILABLE,    // 5xx, timeout, network: worth retrying with backoff
        BAD_RESPONSE,   // reply was empty, blocked, cut off, or not valid JSON
        CONFIG          // bad key / model / request (400, 401, 403, 404): retrying won't help
    }

    private final Kind kind;
    private final Duration retryAfter;

    public AiProviderException(Kind kind, String message) {
        this(kind, message, (Duration) null);
    }

    public AiProviderException(Kind kind, String message, Duration retryAfter) {
        super(message);
        this.kind = kind;
        this.retryAfter = retryAfter;
    }

    public AiProviderException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.retryAfter = null;
    }

    public Kind kind() { return kind; }

    /** From the Retry-After header; null if the server didn't say. */
    public Duration retryAfter() { return retryAfter; }
}