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
 * Restricts the local signature service to loopback clients and reproduces the
 * CORS headers expected by the legacy NexU JavaScript client.
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

        applyCorsHeaders(request, response, api.getAppConfig());

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

    private static void applyCorsHeaders(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AppConfig config) {

        final String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (config.isCorsAllowAllOrigins()) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        } else {
            final Set<String> allowedOrigins = config.getCorsAllowedOrigins();
            if (origin != null && allowedOrigins != null && allowedOrigins.contains(origin)) {
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            }
        }

        response.setHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "OPTIONS, GET, POST");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, HttpHeaders.CONTENT_TYPE);
    }
}
