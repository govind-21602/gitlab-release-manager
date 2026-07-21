package com.releasemanager.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * Utility class providing a generic retry mechanism with exponential backoff.
 */
public final class RetryUtil {

    private static final Logger log = LoggerFactory.getLogger(RetryUtil.class);

    private RetryUtil() {}

    /**
     * Executes the given {@link Callable} with retry logic.
     *
     * <p>On each failure the delay doubles (exponential backoff).
     * The original exception is re-thrown after all attempts are exhausted.
     *
     * @param callable      the operation to execute
     * @param maxAttempts   maximum number of attempts (must be >= 1)
     * @param initialDelay  initial delay between retries in milliseconds
     * @param operationName human-readable name used in log messages
     * @param <T>           return type of the callable
     * @return the result of the callable on success
     * @throws Exception if all attempts fail
     */
    public static <T> T execute(
            Callable<T> callable,
            int maxAttempts,
            long initialDelay,
            String operationName
    ) throws Exception {

        long delay = initialDelay;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callable.call();
            } catch (Exception ex) {
                lastException = ex;
                if (attempt == maxAttempts) {
                    log.error("[{}] Failed after {} attempt(s): {}",
                            operationName, maxAttempts, ex.getMessage());
                } else {
                    log.warn("[{}] Attempt {}/{} failed: {}. Retrying in {}ms...",
                            operationName, attempt, maxAttempts, ex.getMessage(), delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                    delay *= 2;
                }
            }
        }
        throw lastException;
    }

    /**
     * Convenience overload with default 3 attempts and 2-second initial delay.
     *
     * @param callable      the operation to execute
     * @param operationName human-readable name used in log messages
     * @param <T>           return type of the callable
     * @return the result of the callable on success
     * @throws Exception if all attempts fail
     */
    public static <T> T execute(Callable<T> callable, String operationName)
            throws Exception {
        return execute(callable, 3, 2_000L, operationName);
    }
}
