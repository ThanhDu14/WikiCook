package com.wikicook.auth.controller;

import com.wikicook.auth.config.AuthProperties;
import com.wikicook.auth.security.SafeRedirects;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/v1/auth/oauth2/google} for the cases where Spring's redirect filter does not take the request
 * (spec 010, research R7). When Google is configured the filter answers first and sends the browser to Google, so a
 * request that reaches this method means: Google is <b>not configured</b> (back to the sign-in page, e-mail sign-in
 * unaffected, NFR-005), or the call is a re-authentication ({@code intent=reauth}), which is not built yet and is
 * answered as a cancelled one, so the user stays signed in on the page that asked (FR-022b).
 */
@RestController
@RequestMapping("/api/v1/auth/oauth2")
public class GoogleStartController {

    private final String baseUrl;

    public GoogleStartController(AuthProperties properties) {
        String base = properties.publicBaseUrl();
        this.baseUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    @GetMapping("/google")
    public void start(@RequestParam(required = false) String redirect, @RequestParam(required = false) String intent,
            HttpServletResponse response) {
        String target;
        if ("reauth".equalsIgnoreCase(intent)) {
            String back = SafeRedirects.sanitize(redirect);
            target = (back == null ? "/" : back) + (back != null && back.contains("?") ? "&" : "?") + "reauth=cancelled";
        } else {
            target = "/login?error=google_unavailable";
        }
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader(HttpHeaders.LOCATION, baseUrl + target);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }
}
