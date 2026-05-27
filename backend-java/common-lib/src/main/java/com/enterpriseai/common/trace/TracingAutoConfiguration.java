package com.enterpriseai.common.trace;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-configuration that registers OpenTelemetry tracing across all services.
 *
 * Registers:
 *   - TraceFilter — spans for every incoming HTTP request
 *   - RestTemplateTraceInterceptor — spans for outbound HTTP calls
 *   - JaegerConfig hook — enables per-service Jaeger endpoint
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.opentelemetry.api.OpenTelemetry")
public class TracingAutoConfiguration {

    /** Filter that wraps every HTTP request in an OTEL span. */
    @Bean
    public FilterRegistrationBean<TraceFilter> traceFilterRegistration() {
        FilterRegistrationBean<TraceFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new TraceFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(Integer.MIN_VALUE + 100); // run early, after Spring Security
        reg.setName("otelTraceFilter");
        return reg;
    }

    /** Interceptor for RestTemplate outbound tracing. */
    @Bean
    public RestTemplateTraceInterceptor restTemplateTraceInterceptor() {
        return new RestTemplateTraceInterceptor();
    }

    /** Auto-wired RestTemplate with trace interceptor pre-configured. */
    @Bean
    public RestTemplate tracedRestTemplate(RestTemplateBuilder builder,
                                           RestTemplateTraceInterceptor interceptor) {
        return builder
                .additionalInterceptors(interceptor)
                .build();
    }
}
