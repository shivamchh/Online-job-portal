package com.example.Online_job_portal.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

/**
 * Enforces role-based access control (RBAC) across both MVC view controllers
 * and action Servlets. Verifies session authentication and authorization.
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/css/",
            "/js/",
            "/images/",
            "/favicon.ico"
    );

    private static final Set<String> PUBLIC_EXACT = Set.of(
            "/",
            "/login",
            "/register",
            "/logout",
            "/access-denied",
            "/error",
            "/servlet/login",
            "/servlet/register",
            "/servlet/logout",
            "/servlet/statistics",
            "/api/statistics",
            "/servlet/activities",
            "/api/activities",
            "/api/jobs",
            "/api/applications",
            "/jobseeker/jobs" // allow public browsing of open jobs
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI().substring(req.getContextPath().length());

        // Allow public static assets and exact public endpoints
        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        Object userId = session != null ? session.getAttribute("userId") : null;
        Object role = session != null ? session.getAttribute("role") : null;

        // If not logged in
        if (userId == null || role == null) {
            String accept = req.getHeader("Accept");
            String requestedWith = req.getHeader("X-Requested-With");
            if (path.startsWith("/api/") || (accept != null && accept.contains("application/json")) || "XMLHttpRequest".equals(requestedWith)) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType("application/json");
                resp.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                return;
            }
            resp.setStatus(HttpServletResponse.SC_FOUND);
            resp.sendRedirect(req.getContextPath() + "/login?error=auth_required");
            return;
        }

        String userRole = role.toString().toUpperCase();

        // Enforce Admin role for admin views and admin servlets
        if (path.startsWith("/admin") || path.equals("/servlet/settings") || path.equals("/servlet/users")) {
            if (!"ADMIN".equals(userRole)) {
                sendForbiddenOrRedirect(req, resp, path);
                return;
            }
        }

        // Enforce Employer role for employer views and employer servlets (Admin can also manage)
        if (path.startsWith("/employer") || path.equals("/servlet/jobs") || path.equals("/servlet/applications")) {
            if (!"EMPLOYER".equals(userRole) && !"ADMIN".equals(userRole)) {
                sendForbiddenOrRedirect(req, resp, path);
                return;
            }
        }

        // Enforce Job Seeker role for seeker views and seeker servlets
        if ((path.startsWith("/jobseeker") && !path.equals("/jobseeker/jobs"))
                || path.equals("/servlet/resume-upload")
                || path.equals("/servlet/profile")
                || path.equals("/servlet/notifications")) {
            if (!"JOB_SEEKER".equals(userRole)) {
                sendForbiddenOrRedirect(req, resp, path);
                return;
            }
        }

        // Common authenticated endpoints (/servlet/messages, /servlet/resume-download) pass through
        chain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        if (PUBLIC_EXACT.contains(path)) {
            return true;
        }
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void sendForbiddenOrRedirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        String accept = req.getHeader("Accept");
        String requestedWith = req.getHeader("X-Requested-With");
        if (path.startsWith("/api/") || (accept != null && accept.contains("application/json")) || "XMLHttpRequest".equals(requestedWith)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Access denied for role\"}");
        } else {
            resp.setStatus(HttpServletResponse.SC_FOUND);
            resp.sendRedirect(req.getContextPath() + "/access-denied");
        }
    }
}
