package lu.nowina.nexu.springboot.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lu.nowina.nexu.api.AppConfig;
import lu.nowina.nexu.api.NexuAPI;

/**
 * Restricts the signature service to loopback clients. Legacy routes retain the
 * historical CORS behaviour, while the modern /v1 protocol requires an
 * explicit browser-origin allowlist.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
final class NexuLoopbackCorsFilter extends OncePerRequestFilter {

    private final NexuAPI api;

    NexuLoopbackCorsFilter(final NexuAPI api) {
        this.api = api;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        if (!isLoopback(request.getRemoteAddr())) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Please connect from localhost");
            return;
        }

        if (!applyCorsHeaders(request, response, api.getAppConfig())) {
            response.sendError(
                    HttpStatus.FORBIDDEN.value(),
                    "Browser origin is not allowed. Configure cors_allowed_origin with an explicit origin for /v1.");
            return;
        }

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            response.setStatus(HttpStatus.OK.value());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isLoopback(final String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return false;
        }
        try {
            return InetAddress.getByName(remoteAddress).isLoopbackAddress();
        } catch (final Exception e) {
            return false;
        }
    }

    /**
     * @return true when the request may continue
     */
    private static boolean applyCorsHeaders(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AppConfig config) {

        final String origin = request.getHeader(HttpHeaders.ORIGIN);
        final boolean modernApi = modernApiRequest(request);

        // Non-browser clients do not send Origin. They are still constrained to
        // loopback by the check above.
        if (origin == null || origin.isBlank()) {
            applyCommonCorsHeaders(response);
            return true;
        }

        if (config.isCorsAllowAllOrigins()) {
            if (modernApi) {
                return false;
            }
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            applyCommonCorsHeaders(response);
            return true;
        }

        final Set<String> allowedOrigins = config.getCorsAllowedOrigins();
        if (allowedOrigins == null || !allowedOrigins.contains(origin)) {
            return false;
        }

        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        applyCommonCorsHeaders(response);
        return true;
    }

    private static boolean modernApiRequest(final HttpServletRequest request) {
        String path = request.getRequestURI();
        final String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.equals("/v1") || path.startsWith("/v1/");
    }

    private static void applyCommonCorsHeaders(final HttpServletResponse response) {
        response.setHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "OPTIONS, GET, POST");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, HttpHeaders.CONTENT_TYPE);
    }
}
