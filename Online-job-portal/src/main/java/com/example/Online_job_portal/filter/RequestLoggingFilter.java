package com.example.Online_job_portal.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Filter measuring request execution performance and auditing HTTP traffic.
 * Records HTTP Method, Request Path, HTTP Status code, and total processing duration in milliseconds.
 */
@WebFilter("/*")
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        long startTime = System.currentTimeMillis();
        String uri = req.getRequestURI();
        String method = req.getMethod();

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            // Only log non-static asset requests to prevent noise
            if (!uri.startsWith("/css/") && !uri.startsWith("/js/") && !uri.startsWith("/images/") && !uri.endsWith(".ico")) {
                log.info("[HTTP-AUDIT] {} {} - Status: {} - {}ms", method, uri, resp.getStatus(), duration);
            }
        }
    }
}
