package com.consultant.worklog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        // Log request metadata before processing
        logRequestMetadata(requestWrapper);

        // Process the request
        filterChain.doFilter(requestWrapper, responseWrapper);

        long duration = System.currentTimeMillis() - startTime;

        // Log request body after processing (now it's cached)
        logRequestBody(requestWrapper);

        // Log response
        logResponse(responseWrapper, duration);

        responseWrapper.copyBodyToResponse();
    }

    private void logRequestMetadata(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String queryString = request.getQueryString();

        log.info("=== INCOMING HTTP REQUEST ===");
        log.info("Method: {}", method);
        log.info("Path: {}{}", path, queryString != null ? "?" + queryString : "");

        // Log headers
        log.debug("Headers:");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            // Don't log sensitive headers
            if (!headerName.equalsIgnoreCase("Authorization") && !headerName.equalsIgnoreCase("Cookie")) {
                log.debug("  {}: {}", headerName, request.getHeader(headerName));
            }
        }
    }

    private void logRequestBody(ContentCachingRequestWrapper request) {
        // Request body logging disabled - details are logged in service layer where relevant
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        int status = response.getStatus();
        log.info("Response Status: {}", status);
        log.info("Duration: {} ms", duration);
        log.info("=== REQUEST COMPLETED ===");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Don't log health check endpoints
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || path.startsWith("/health");
    }
}
