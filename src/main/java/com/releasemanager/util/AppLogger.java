package com.releasemanager.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for application-wide logging helpers.
 *
 * <p>Provides a factory method for named loggers and ensures the log
 * directory exists before the first log file is written.
 */
public final class AppLogger {

    private AppLogger() {}

    /**
     * Returns a named SLF4J {@link Logger} for the given class.
     *
     * @param clazz the class requesting the logger
     * @return the logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * Returns a named SLF4J {@link Logger} for the given name.
     *
     * @param name the logger name
     * @return the logger instance
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

    /**
     * Ensures that the specified log directory exists, creating it (and any
     * intermediate directories) if necessary.
     *
     * @param logDir path to the log directory
     * @throws IOException if the directory cannot be created
     */
    public static void ensureLogDirectory(String logDir) throws IOException {
        Path path = Path.of(logDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            LoggerFactory.getLogger(AppLogger.class)
                    .info("Created log directory: {}", path.toAbsolutePath());
        }
    }
}
