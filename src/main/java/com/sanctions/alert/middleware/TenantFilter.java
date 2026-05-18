package com.sanctions.alert.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Extracts the {@code X-Tenant-ID} header and stores it in {@link TenantContext}.
 *
 * Rejects requests with a missing or blank header with 400 Bad Request
 * before they reach any controller — a single enforcement point.
 *
 * Design rationale (see README):
 *   - Header chosen over JWT claim or path segment for simplicity; the
 *     interface does not mention authentication, so we treat tenantId as
 *     a routing/isolation key supplied by a trusted upstream gateway.
 *   - Enforcement at the filter layer means no controller or service method
 *     can accidentally skip the check.
 */
@Component
@Order(1)
public class TenantFilter implements Filter {

    private static final String HEADER = "X-Tenant-ID";
    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req = (HttpServletRequest)  request;
        HttpServletResponse res = (HttpServletResponse) response;

        String tenantId = req.getHeader(HEADER);

        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Request rejected: missing {} header on {} {}", HEADER, req.getMethod(), req.getRequestURI());
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write(mapper.writeValueAsString(
                    Map.of("error", "Missing required header: " + HEADER)
            ));
            return;
        }

        TenantContext.set(tenantId.trim());
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
