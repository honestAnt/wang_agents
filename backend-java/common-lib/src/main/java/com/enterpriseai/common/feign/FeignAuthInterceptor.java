package com.enterpriseai.common.feign;

import com.enterpriseai.common.auth.JwtContext;
import com.enterpriseai.common.auth.SecurityContextHolder;
import com.enterpriseai.common.trace.TraceContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Feign interceptor that automatically propagates JWT and trace_id
 * to downstream services.
 */
public class FeignAuthInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeignAuthInterceptor.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String USER_HEADER = "X-User-Id";

    @Override
    public void apply(RequestTemplate template) {
        JwtContext jwtCtx = SecurityContextHolder.get();
        if (jwtCtx != null) {
            template.header(TENANT_HEADER, jwtCtx.getTenantId());
            template.header(USER_HEADER, jwtCtx.getUserId());
        }

        String traceId = TraceContext.getTraceId();
        if (traceId != null) {
            template.header(TRACE_HEADER, traceId);
        }

        log.debug("Feign request: {} {}, tenant={}, trace={}",
                template.method(), template.url(),
                jwtCtx != null ? jwtCtx.getTenantId() : "null",
                traceId);
    }
}
