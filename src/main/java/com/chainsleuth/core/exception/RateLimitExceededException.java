package com.chainsleuth.core.exception;

/**
 * Domain exception thrown when outbound RPC quotas, external explorer rate limits,
 * or inbound client API quotas are exhausted.
 * <p>
 * Carries the required {@code retryAfterSeconds} metric used by {@link GlobalExceptionHandler}
 * to emit standard HTTP {@code Retry-After} response headers.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1C — Global Exception Handling &amp; RFC 9457 Infrastructure</li>
 *   <li><b>Platform Component:</b> Rate Limiting Exception</li>
 *   <li><b>Interacts with:</b> {@link ErrorCode}, {@link ChainSleuthException}, {@link GlobalExceptionHandler}</li>
 * </ul>
 */
public final class RateLimitExceededException extends ChainSleuthException {

    private final int retryAfterSeconds;
    private final String limitType;

    /**
     * Constructs a rate limit exception with explicit cooldown requirements.
     *
     * @param limitType         subsystem or gateway throttled (e.g., "API", "BASE_RPC", "ETHERSCAN")
     * @param retryAfterSeconds duration in seconds to wait before retrying
     */
    public RateLimitExceededException(String limitType, int retryAfterSeconds) {
        super(
                ErrorCode.RPC_RATE_LIMIT,
                "Rate limit exceeded for " + limitType + ". Please retry after " + retryAfterSeconds + " seconds."
        );
        this.retryAfterSeconds = retryAfterSeconds;
        this.limitType = limitType;
    }

    public int retryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String limitType() {
        return limitType;
    }
}
