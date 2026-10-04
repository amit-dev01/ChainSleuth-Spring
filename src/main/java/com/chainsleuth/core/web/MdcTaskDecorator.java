package com.chainsleuth.core.web;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * Task decorator propagating SLF4J {@link MDC} context maps across asynchronous thread boundaries.
 * <p>
 * <b>The Concurrency Challenge:</b><br>
 * SLF4J's MDC relies on {@link ThreadLocal} storage. In high-concurrency environments utilizing
 * Java 25 virtual threads or thread pool executors, child tasks dispatched asynchronously do not
 * automatically inherit the diagnostic context (such as {@code traceId}, {@code requestId}, or {@code uri})
 * of the originating request thread.
 * </p>
 * <p>
 * <b>The Solution:</b><br>
 * {@link MdcTaskDecorator} captures a point-in-time immutable snapshot of the calling thread's MDC map
 * and binds it to the executing virtual thread for the duration of the task, ensuring consistent log
 * correlation before explicitly purging the context upon completion.
 * </p>
 * <p>
 * <b>Example Usage on an Executor:</b>
 * <pre>{@code
 * ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 * executor.setTaskDecorator(new MdcTaskDecorator());
 * executor.initialize();
 * }</pre>
 * </p>
 *
 * <ul>
 *   <li><b>Phase:</b> Phase 1C — Global Exception Handling &amp; RFC 9457 Infrastructure</li>
 *   <li><b>Platform Component:</b> Asynchronous Context Propagation Decorator</li>
 *   <li><b>Interacts with:</b> {@link RequestLoggingFilter}, Phase 3 BlockchainTraversalService</li>
 * </ul>
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> mdcSnapshot = MDC.getCopyOfContextMap();

        return () -> {
            if (mdcSnapshot != null) {
                MDC.setContextMap(mdcSnapshot);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
