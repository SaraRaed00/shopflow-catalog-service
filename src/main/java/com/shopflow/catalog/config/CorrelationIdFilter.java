package com.shopflow.catalog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // ensure this runs very early (once the request starts)
public class CorrelationIdFilter extends OncePerRequestFilter // class that runs once per request before it reaches the controllers
{

    private static final String HEADER_NAME = "X-Request-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER_NAME); // request correlation id generated or propagated from an X-Request-Id header
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId); // here every log statement will include this value
        response.setHeader(HEADER_NAME, correlationId); // echoes the id back to the client, so we can search the logs with id
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY); // a must cuz the next unrelated request handled by the same reused thread could accidentally inherit the previous request's correlation id
        }
    }
}
