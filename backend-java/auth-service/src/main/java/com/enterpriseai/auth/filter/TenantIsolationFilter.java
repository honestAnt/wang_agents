package com.enterpriseai.auth.filter;

import com.enterpriseai.common.auth.JwtContext;
import com.enterpriseai.common.auth.SecurityContextHolder;
import com.enterpriseai.common.trace.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Extracts tenant_id, user_id, and trace_id from JWT and request headers,
 * populating ThreadLocal context for downstream use.
 */
@Component
public class TenantIsolationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantIsolationFilter.class);

    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            // Initialize trace context
            String traceId = request.getHeader(TRACE_HEADER);
            if (traceId != null && !traceId.isBlank()) {
                TraceContext.init(traceId);
            } else {
                TraceContext.init();
            }

            // Extract tenant/user from JWT
            SecurityContext secCtx = org.springframework.security.core.context
                    .SecurityContextHolder.getContext();
            Authentication auth = secCtx != null ? secCtx.getAuthentication() : null;
            Object principal = auth != null ? auth.getPrincipal() : null;

            String userId = null;
            String tenantId = request.getHeader(TENANT_HEADER);
            String username = null;
            List<String> roles = Collections.emptyList();

            if (principal instanceof Jwt jwt) {
                userId = jwt.getSubject();
                username = jwt.getClaimAsString("preferred_username");
                if (tenantId == null || tenantId.isBlank()) {
                    tenantId = jwt.getClaimAsString("tenant_id");
                }
                Map<String, Object> realmAccess = jwt.getClaim("realm_access");
                if (realmAccess != null) {
                    @SuppressWarnings("unchecked")
                    List<String> r = (List<String>) realmAccess.get("roles");
                    if (r != null) roles = r;
                }
            }

            JwtContext ctx = JwtContext.builder()
                    .userId(userId != null ? userId : "anonymous")
                    .username(username != null ? username : "anonymous")
                    .tenantId(tenantId != null ? tenantId : "default")
                    .roles(roles)
                    .build();
            SecurityContextHolder.set(ctx);

            // Add trace header to response
            response.setHeader(TRACE_HEADER, TraceContext.getTraceId());

            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clear();
            TraceContext.clear();
        }
    }
}
