package com.enterpriseai.common.auth;

/**
 * ThreadLocal holder for the current request's JWT context.
 * Populated by the API Gateway's JWT auth filter and the Feign interceptor.
 */
public final class SecurityContextHolder {

    private static final ThreadLocal<JwtContext> CONTEXT = new ThreadLocal<>();

    private SecurityContextHolder() {}

    public static void set(JwtContext context) {
        CONTEXT.set(context);
    }

    public static JwtContext get() {
        return CONTEXT.get();
    }

    public static String getTenantId() {
        JwtContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getTenantId() : null;
    }

    public static String getUserId() {
        JwtContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getUserId() : null;
    }

    public static String getUsername() {
        JwtContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getUsername() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
