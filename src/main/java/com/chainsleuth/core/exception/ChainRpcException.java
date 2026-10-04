package com.chainsleuth.core.exception;

/**
 * Domain exception capturing failures when dispatching RPC or REST requests to upstream blockchain nodes.
 * <p>
 * Evaluates upstream HTTP status codes (such as 429 Rate Limited or 504 Gateway Timeout)
 * to automatically map to corresponding {@link ErrorCode} classifications.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1C — Global Exception Handling &amp; RFC 9457 Infrastructure</li>
 *   <li><b>Platform Component:</b> Blockchain RPC Integration Exception</li>
 *   <li><b>Interacts with:</b> {@link ErrorCode}, {@link ChainSleuthException}</li>
 * </ul>
 */
public final class ChainRpcException extends ChainSleuthException {

    private final String chainName;
    private final String rpcMethod;
    private final int upstreamStatusCode;

    /**
     * Constructs a failure mapped directly from an upstream HTTP response status.
     *
     * @param chainName          blockchain network identifier (e.g. "BASE", "ETHEREUM")
     * @param rpcMethod          remote procedure or REST endpoint attempted
     * @param upstreamStatusCode raw HTTP status code returned by the node
     * @param upstreamMessage    raw message from node payload (sanitized and truncated)
     */
    public ChainRpcException(String chainName, String rpcMethod, int upstreamStatusCode, String upstreamMessage) {
        super(
                resolveErrorCode(upstreamStatusCode),
                "RPC call '" + rpcMethod + "' on " + chainName + " failed. Upstream status: "
                        + upstreamStatusCode + ". Message: " + truncate(upstreamMessage, 200)
        );
        this.chainName = chainName;
        this.rpcMethod = rpcMethod;
        this.upstreamStatusCode = upstreamStatusCode;
    }

    /**
     * Constructs a connection or transport failure with root cause preservation.
     *
     * @param chainName blockchain network identifier
     * @param rpcMethod remote procedure or REST endpoint attempted
     * @param message   descriptive failure message
     * @param cause     underlying connection or protocol error
     */
    public ChainRpcException(String chainName, String rpcMethod, String message, Throwable cause) {
        super(
                ErrorCode.RPC_FAILURE,
                "RPC call '" + rpcMethod + "' on " + chainName + " failed: " + truncate(message, 200),
                cause
        );
        this.chainName = chainName;
        this.rpcMethod = rpcMethod;
        this.upstreamStatusCode = 0;
    }

    /**
     * Resolves the appropriate {@link ErrorCode} from the upstream HTTP status.
     *
     * @param upstreamStatus HTTP status from blockchain node
     * @return appropriate {@link ErrorCode}
     */
    private static ErrorCode resolveErrorCode(int upstreamStatus) {
        return switch (upstreamStatus) {
            case 429 -> ErrorCode.RPC_RATE_LIMIT;
            case 504, 408 -> ErrorCode.RPC_TIMEOUT;
            default -> ErrorCode.RPC_FAILURE;
        };
    }

    /**
     * Truncates message content to avoid polluting error details or client responses.
     *
     * @param s   raw string
     * @param max character threshold
     * @return truncated string
     */
    private static String truncate(String s, int max) {
        if (s == null) {
            return "(no message)";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    public String chainName() {
        return chainName;
    }

    public String rpcMethod() {
        return rpcMethod;
    }

    public int upstreamStatusCode() {
        return upstreamStatusCode;
    }
}
