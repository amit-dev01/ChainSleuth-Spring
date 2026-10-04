package com.chainsleuth.core.exception;

/**
 * Domain exception capturing failures within Spring AI and Google GenAI LLM subsystems.
 * <p>
 * Manages model downtime, response parsing failures, token context exhaustion,
 * and AI function/tool call execution errors.
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1C — Global Exception Handling &amp; RFC 9457 Infrastructure</li>
 *   <li><b>Platform Component:</b> AI Subsystem Exception</li>
 *   <li><b>Interacts with:</b> {@link ErrorCode}, {@link ChainSleuthException}</li>
 * </ul>
 */
public final class AiServiceException extends ChainSleuthException {

    private final String modelName;

    /**
     * Private constructor called by static factory methods.
     *
     * @param code      AI error code classification
     * @param modelName name of LLM model (e.g. "gemini-2.5-flash")
     * @param detail    human-readable detail
     * @param cause     optional root cause throwable
     */
    private AiServiceException(ErrorCode code, String modelName, String detail, Throwable cause) {
        super(code, detail, cause);
        this.modelName = modelName;
    }

    /**
     * Creates an exception representing upstream model unavailability or outage.
     *
     * @param modelName target AI model
     * @param cause     network or HTTP 5xx cause
     * @return initialized {@link AiServiceException}
     */
    public static AiServiceException modelUnavailable(String modelName, Throwable cause) {
        return new AiServiceException(
                ErrorCode.MODEL_UNAVAILABLE,
                modelName,
                "The AI model '" + modelName
                        + "' is currently unavailable. The analysis cannot be completed. Please try again in a few minutes.",
                cause
        );
    }

    /**
     * Creates an exception when generative output cannot be mapped to the expected forensic schema.
     * Note: Raw model outputs are intentionally omitted to avoid leaking sensitive investigative text.
     *
     * @param modelName target AI model
     * @param rawOutput unparseable raw output string (omitted from client response)
     * @return initialized {@link AiServiceException}
     */
    public static AiServiceException outputParseFailed(String modelName, String rawOutput) {
        return new AiServiceException(
                ErrorCode.OUTPUT_PARSE_FAILURE,
                modelName,
                "The AI model '" + modelName
                        + "' returned output that could not be parsed into the required forensic brief format. Please retry the analysis.",
                null
        );
    }

    /**
     * Creates an exception when an automated forensic tool or Neo4j Cypher call invoked by the model fails.
     *
     * @param modelName target AI model
     * @param toolName  name of tool invoked
     * @param cause     underlying failure cause
     * @return initialized {@link AiServiceException}
     */
    public static AiServiceException toolCallFailed(String modelName, String toolName, Throwable cause) {
        return new AiServiceException(
                ErrorCode.TOOL_CALL_FAILURE,
                modelName,
                "AI tool '" + toolName + "' failed during forensic analysis with model '" + modelName
                        + "'. Investigation may be incomplete.",
                cause
        );
    }

    /**
     * Creates an exception when the input prompt and hop history exceed model context boundaries.
     *
     * @param modelName   target AI model
     * @param inputTokens computed input token count
     * @param maxTokens   model maximum context window
     * @return initialized {@link AiServiceException}
     */
    public static AiServiceException contextTooLarge(String modelName, int inputTokens, int maxTokens) {
        return new AiServiceException(
                ErrorCode.CONTEXT_TOO_LARGE,
                modelName,
                "The investigation input (" + inputTokens + " tokens) exceeds the maximum context length ("
                        + maxTokens + " tokens) for model '" + modelName + "'. Reduce traversal depth or scope.",
                null
        );
    }

    public String modelName() {
        return modelName;
    }
}
