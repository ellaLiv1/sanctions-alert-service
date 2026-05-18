package com.sanctions.alert.middleware;

/**
 * Carries the resolved tenant ID for the duration of a single request thread.
 *
 * Set by {@link TenantFilter} before the request reaches any handler.
 * Cleared in a finally block to prevent leakage across thread pool reuse.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String get() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
