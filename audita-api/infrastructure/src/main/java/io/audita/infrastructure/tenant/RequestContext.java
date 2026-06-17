package io.audita.infrastructure.tenant;

/**
 * Request-scoped context for data captured from the incoming HTTP request
 * that is needed by downstream infrastructure services (audit logging, etc.).
 */
public final class RequestContext {

    private static final ThreadLocal<String> CURRENT_IP = new ThreadLocal<>();

    private RequestContext() {}

    public static void setCurrentIp(String ip) {
        CURRENT_IP.set(ip);
    }

    public static String getCurrentIp() {
        return CURRENT_IP.get();
    }

    public static void clear() {
        CURRENT_IP.remove();
    }
}
