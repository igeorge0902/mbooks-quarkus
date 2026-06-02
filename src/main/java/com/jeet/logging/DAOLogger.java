package com.jeet.logging;

/**
 * @deprecated DAOLogger is no longer used.
 *
 * <p>Previously provided static structured logging for non-CDI DAO calls
 * ({@code DAO.instance()} path in mbooks-quarkus).
 *
 * <p>All DAO logging is now handled by {@link ObservedLogInterceptor} via CDI proxy.
 * The DAO class is annotated with {@link ObservedLog} on every public method and is
 * injected via {@code @Inject DAO dao} in all callers.
 *
 * <p>This class is retained as a tombstone to document the migration.
 * It may be deleted once the mbooks-quarkus logging policy is confirmed stable.
 *
 * @see ObservedLog
 * @see ObservedLogInterceptor
 */
@Deprecated(since = "2026-06-02", forRemoval = true)
public final class DAOLogger {

    private DAOLogger() {}
}
