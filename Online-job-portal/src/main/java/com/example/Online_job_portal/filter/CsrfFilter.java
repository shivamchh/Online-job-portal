package com.example.Online_job_portal.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * Filter providing Cross-Site Request Forgery (CSRF) protection for state-changing HTTP requests.
 * Issues and validates cryptographic tokens stored in the user's active session.
 */
public class CsrfFilter implements Filter {

    public static final String CSRF_TOKEN_SESSION_ATTR = "CSRF_TOKEN";
    public static final String CSRF_TOKEN_PARAM = "_csrf";
    public static final String CSRF_TOKEN_HEADER = "X-CSRF-TOKEN";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Endpoints exempt from CSRF token verification (e.g. initial public login/register forms before session)
    private static final Set<String> EXEMPT_ENDPOINTS = Set.of(
            "/login",
            "/register",
            "/servlet/login",
            "/servlet/register"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(true);
        String token = (String) session.getAttribute(CSRF_TOKEN_SESSION_ATTR);
        if (token == null) {
            byte[] bytes = new byte[24];
            SECURE_RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(CSRF_TOKEN_SESSION_ATTR, token);
        }

        // Expose token for Thymeleaf templates and request attributes
        req.setAttribute("_csrf", token);
        req.setAttribute("csrfToken", token);

        String method = req.getMethod();
        String path = req.getRequestURI().substring(req.getContextPath().length());
        int semicolon = path.indexOf(';');
        if (semicolon != -1) {
            path = path.substring(0, semicolon);
        }

        // Validate state-changing methods
        if (isStateChanging(method) && !isExempt(path)) {
            String submittedToken = req.getParameter(CSRF_TOKEN_PARAM);
            if (submittedToken == null || submittedToken.isBlank()) {
                submittedToken = req.getParameter("csrfToken");
            }
            if (submittedToken == null || submittedToken.isBlank()) {
                submittedToken = req.getHeader(CSRF_TOKEN_HEADER);
            }
            if (submittedToken == null || submittedToken.isBlank()) {
                submittedToken = req.getHeader("X-XSRF-TOKEN");
            }

            if (submittedToken == null || !submittedToken.equals(token)) {
                String accept = req.getHeader("Accept");
                if (path.startsWith("/api/") || (accept != null && accept.contains("application/json"))) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.setContentType("application/json");
                    resp.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Invalid or missing CSRF token\"}");
                } else {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token. Please refresh and try again.");
                }
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isStateChanging(String method) {
        return "POST".equalsIgnoreCase(method) ||
               "PUT".equalsIgnoreCase(method) ||
               "DELETE".equalsIgnoreCase(method) ||
               "PATCH".equalsIgnoreCase(method);
    }

    private boolean isExempt(String path) {
        return EXEMPT_ENDPOINTS.contains(path) || path.startsWith("/actuator");
    }
}
