package com.matheusdev.mindforge.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            if (request.isAsyncStarted()) {
                request.getAsyncContext().addListener(new jakarta.servlet.AsyncListener() {
                    @Override
                    public void onComplete(jakarta.servlet.AsyncEvent asyncEvent) throws IOException {
                        logRequestAndResponse(request, requestWrapper, responseWrapper, startTime);
                        responseWrapper.copyBodyToResponse();
                    }

                    @Override
                    public void onTimeout(jakarta.servlet.AsyncEvent asyncEvent) {
                    }

                    @Override
                    public void onError(jakarta.servlet.AsyncEvent asyncEvent) {
                    }

                    @Override
                    public void onStartAsync(jakarta.servlet.AsyncEvent asyncEvent) {
                    }
                });
            } else {
                logRequestAndResponse(request, requestWrapper, responseWrapper, startTime);
                responseWrapper.copyBodyToResponse();
            }
        }
    }

    private void logRequestAndResponse(HttpServletRequest request,
            ContentCachingRequestWrapper requestWrapper,
            ContentCachingResponseWrapper responseWrapper,
            long startTime) {
        long duration = System.currentTimeMillis() - startTime;

        // Log Request
        String requestBody = new String(requestWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        log.info("REQUEST: method=[{}] uri=[{}] body=[{}]",
                request.getMethod(), request.getRequestURI(), requestBody);

        // Log Response
        String responseBody = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        log.info("RESPONSE: status=[{}] duration=[{}ms] body=[{}]",
                responseWrapper.getStatus(), duration, responseBody);
    }
}
