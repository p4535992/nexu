package lu.nowina.nexu.springboot.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import lu.nowina.nexu.api.plugin.HttpRequest;

final class SpringHttpRequest implements HttpRequest {

    private final HttpServletRequest request;
    private final String target;

    SpringHttpRequest(final HttpServletRequest request, final String target) {
        this.request = Objects.requireNonNull(request, "request");
        this.target = Objects.requireNonNull(target, "target");
    }

    @Override
    public String getParameter(final String name) {
        return request.getParameter(name);
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public InputStream getInputStream() {
        try {
            return request.getInputStream();
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot read the HTTP request body", e);
        }
    }
}
