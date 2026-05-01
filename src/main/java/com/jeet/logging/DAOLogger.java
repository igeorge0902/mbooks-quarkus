package com.jeet.logging;

import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

/**
 * Static structured logger for non-CDI DAO calls (mbooks-quarkus DAO.instance() path).
 *
 * <p>Because mbooks-quarkus DAO is a manual singleton ({@code DAO.instance()}),
 * it bypasses the CDI proxy and the {@link ObservedLogInterceptor} will NOT fire for it.
 * This helper replicates the same structured log schema so both services emit consistent events.
 *
 * <p>Usage:
 * <pre>{@code
 * DAOLogger.log(LogLevel.INFO, "LOG-ENTITY", "DAO.getAllMovies", "START", null, null);
 * // ... method body ...
 * DAOLogger.log(LogLevel.INFO, "LOG-ENTITY", "DAO.getAllMovies", "END", null, Map.of("durationMs", elapsed));
 * }</pre>
 *
 * <p>When the DAO is migrated to CDI ({@code @Inject DAO dao}), remove direct calls and rely on
 * {@link ObservedLogInterceptor} instead.
 */
public final class DAOLogger {

    private DAOLogger() {}

    public static void log(LogLevel level, String category, String source,
                           String event, Object[] args, Map<String, Object> extra) {
        Logger catLogger = Logger.getLogger(category);
        String msg = buildMessage(category, source, event, args, extra);
        switch (level) {
            case TRACE -> catLogger.trace(msg);
            case DEBUG -> catLogger.debug(msg);
            case WARN  -> catLogger.warn(msg);
            case ERROR -> catLogger.error(msg);
            default    -> catLogger.info(msg);
        }
    }

    private static String buildMessage(String category, String source, String event,
                                       Object[] args, Map<String, Object> extra) {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append("\"timestamp\":\"").append(Instant.now()).append("\"")
          .append(",\"category\":\"").append(category).append("\"")
          .append(",\"source\":\"").append(source).append("\"")
          .append(",\"event\":\"").append(event).append("\"");

        if (args != null) {
            sb.append(",\"args\":").append(Arrays.toString(args));
        }
        if (extra != null) {
            extra.forEach((k, v) -> sb.append(",\"").append(k).append("\":\"").append(v).append("\""));
        }
        sb.append("}");
        return sb.toString();
    }
}

