package com.jeet.logging;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

/**
 * CDI interceptor for structured observability logging.
 *
 * <p>Emits START / END / ERROR log events for methods annotated with {@link ObservedLog}.
 * Fires only when the bean is called through the CDI proxy (i.e. via {@code @Inject}).
 *
 * <p>Event schema (JSON-like, mapped to Quarkus structured logging):
 * <pre>{@code
 * { "timestamp": "...", "level": "INFO", "category": "LOG-ENTITY",
 *   "source": "DAO.getAllMovies", "event": "START", "durationMs": null }
 * }</pre>
 *
 * <p>File routing is controlled by Quarkus log handlers in {@code application.properties}.
 * Each category maps to a dedicated file — no merged output.
 *
 * <p><b>CDI proxy constraint:</b>
 * This interceptor only fires for CDI-managed calls. mbook-quarkus DAO is {@code @RequestScoped}
 * and injected via {@code @Inject} — interceptor fires correctly without changes.
 */
@ObservedLog(category = "")
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class ObservedLogInterceptor {

    private static final Logger LOGGER = Logger.getLogger(ObservedLogInterceptor.class);

    @AroundInvoke
    public Object around(InvocationContext ctx) throws Exception {
        ObservedLog cfg = resolveAnnotation(ctx);
        if (cfg == null) {
            return ctx.proceed();
        }

        long start = System.currentTimeMillis();
        String source = ctx.getTarget().getClass().getSimpleName()
                + "." + ctx.getMethod().getName();

        emit(cfg.level(), cfg.category(), source, "START",
                cfg.includeArgs() ? ctx.getParameters() : null, null);

        try {
            Object result = ctx.proceed();
            long durationMs = System.currentTimeMillis() - start;

            Map<String, Object> extra = cfg.includeResult() && result != null
                    ? Map.of("durationMs", durationMs, "result", result.toString())
                    : Map.of("durationMs", durationMs);

            emit(cfg.level(), cfg.category(), source, "END", null, extra);
            return result;

        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - start;
            emit(LogLevel.ERROR, cfg.category(), source, "ERROR", null,
                    Map.of("durationMs", durationMs, "error", ex.getClass().getSimpleName()));
            throw ex;
        }
    }

    /**
     * Method-level annotation takes precedence; falls back to type-level.
     */
    private ObservedLog resolveAnnotation(InvocationContext ctx) {
        ObservedLog cfg = ctx.getMethod().getAnnotation(ObservedLog.class);
        if (cfg != null) {
            return cfg;
        }

        Class<?> targetClass = ctx.getTarget().getClass();
        try {
            cfg = targetClass
                    .getMethod(ctx.getMethod().getName(), ctx.getMethod().getParameterTypes())
                    .getAnnotation(ObservedLog.class);
            if (cfg != null) {
                return cfg;
            }
        } catch (NoSuchMethodException ignored) {
            // try superclass below
        }

        Class<?> superClass = targetClass.getSuperclass();
        if (superClass != null) {
            try {
                cfg = superClass
                        .getMethod(ctx.getMethod().getName(), ctx.getMethod().getParameterTypes())
                        .getAnnotation(ObservedLog.class);
                if (cfg != null) {
                    return cfg;
                }
            } catch (NoSuchMethodException ignored) {
                // fall through to type-level lookup
            }
        }

        cfg = targetClass.getAnnotation(ObservedLog.class);
        if (cfg != null) {
            return cfg;
        }
        return superClass != null ? superClass.getAnnotation(ObservedLog.class) : null;
    }

    /**
     * Emits a structured log event to the category logger.
     * The logger name matches the category so Quarkus can route it to the correct file handler.
     */
    private void emit(LogLevel level, String category, String source, String event,
                      Object[] args, Map<String, Object> extra) {
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

    private String buildMessage(String category, String source, String event,
                                Object[] args, Map<String, Object> extra) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{')
          .append("\"timestamp\":").append(toJsonValue(Instant.now().toString()))
          .append(",\"category\":").append(toJsonValue(category))
          .append(",\"source\":").append(toJsonValue(source))
          .append(",\"event\":").append(toJsonValue(event));

        if (args != null) {
            sb.append(",\"args\":").append(toJsonValue(args));
        }
        if (extra != null) {
            extra.forEach((k, v) -> sb.append(',').append(toJsonValue(k)).append(':').append(toJsonValue(v)));
        }
        sb.append('}');
        return sb.toString();
    }

    private String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                sb.append(toJsonValue(String.valueOf(e.getKey())))
                  .append(':')
                  .append(toJsonValue(e.getValue()));
                first = false;
            }
            sb.append('}');
            return sb.toString();
        }
        if (value instanceof Object[] array) {
            return toJsonValue(Arrays.asList(array));
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    sb.append(',');
                }
                sb.append(toJsonValue(item));
                first = false;
            }
            sb.append(']');
            return sb.toString();
        }
        return '"' + escapeJson(String.valueOf(value)) + '"';
    }

    private String escapeJson(String s) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}

