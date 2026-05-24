package com.enterpriseai.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(-1)
public class TraceFilter implements GlobalFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceFilter.class);
    private static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String headerTraceId = exchange.getRequest().getHeaders().getFirst(TRACE_HEADER);
        final String traceId = (headerTraceId != null && !headerTraceId.isBlank())
                ? headerTraceId : UUID.randomUUID().toString();

        exchange.getResponse().getHeaders().add(TRACE_HEADER, traceId);
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.header(TRACE_HEADER, traceId))
                .build();

        String method = mutated.getRequest().getMethod().name();
        String path = mutated.getRequest().getPath().value();
        long start = System.currentTimeMillis();

        return chain.filter(mutated).doFinally(signalType -> {
            long latency = System.currentTimeMillis() - start;
            log.info("{} {} — {}ms", method, path, latency);
        });
    }
}
