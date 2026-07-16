package lu.nowina.nexu.springboot.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lu.nowina.nexu.TechnicalException;
import lu.nowina.nexu.api.Execution;
import lu.nowina.nexu.api.Feedback;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.plugin.HttpPlugin;
import lu.nowina.nexu.api.plugin.HttpResponse;
import lu.nowina.nexu.json.GsonHelper;

@RestController
final class NexuHttpController {

    private static final MediaType JAVASCRIPT =
            MediaType.parseMediaType("text/javascript;charset=UTF-8");

    private final NexuAPI api;

    NexuHttpController(final NexuAPI api) {
        this.api = Objects.requireNonNull(api, "api");
    }

    @GetMapping(path = {"/", "/nexu-info"}, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> nexuInfo() {
        final String version = api.getAppConfig().getApplicationVersion();
        final String body = "{ \"version\": \"" + escapeJson(trim(version)) + "\"}";

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(body);
    }

    @GetMapping(path = "/nexu.js")
    ResponseEntity<String> nexuJavascript(final HttpServletRequest request) throws IOException {
        return javascriptResponse(renderJavascript("/nexu.ftl.js", request));
    }

    @GetMapping(path = "/nexu-v2.js")
    ResponseEntity<String> nexuV2Javascript(final HttpServletRequest request) throws IOException {
        return javascriptResponse(renderJavascript("/nexu-v2.ftl.js", request));
    }

    @GetMapping(path = "/favicon.ico")
    ResponseEntity<byte[]> favicon() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/tray-icon.png")) {
            if (input == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(input.readAllBytes());
        }
    }

    @RequestMapping(path = "/{pluginId}/**")
    ResponseEntity<String> plugin(
            @PathVariable final String pluginId,
            final HttpServletRequest request) throws Exception {

        final HttpPlugin plugin = api.getHttpPlugin(pluginId);
        if (plugin == null) {
            throw new TechnicalException("No HTTP plugin registered with id '" + pluginId + "'");
        }

        final String target = resolvePluginTarget(pluginId, request);
        final HttpResponse pluginResponse = plugin.process(api, new SpringHttpRequest(request, target));
        if (pluginResponse == null || pluginResponse.getContent() == null) {
            throw new TechnicalException("Plugin '" + pluginId + "' responded null");
        }

        final ResponseEntity.BodyBuilder response = ResponseEntity
                .status(HttpStatusCode.valueOf(pluginResponse.getHttpStatus().getHttpCode()));
        if (pluginResponse.getContentType() != null) {
            response.header(HttpHeaders.CONTENT_TYPE, pluginResponse.getContentType());
        }
        return response.body(pluginResponse.getContent());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<String> handleException(final Exception exception) {
        final Execution<Object> execution = new Execution<>(BasicOperationStatus.EXCEPTION);
        final Feedback feedback = new Feedback(exception);
        feedback.setNexuVersion(api.getAppConfig().getApplicationVersion());
        feedback.setInfo(api.getEnvironmentInfo());
        execution.setFeedback(feedback);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(GsonHelper.toJson(execution));
    }

    private String renderJavascript(
            final String resource,
            final HttpServletRequest request) throws IOException {

        String script = readClasspathText(resource);
        script = script.replace("${scheme}", request.getScheme())
                .replace("${nexu_hostname}", api.getAppConfig().getNexuHostname())
                .replace("${nexu_port}", Integer.toString(request.getLocalPort()))
                .replace("${close_token}", Boolean.toString(api.getAppConfig().getCloseToken()));
        return script;
    }

    private static ResponseEntity<String> javascriptResponse(final String script) {
        return ResponseEntity.ok()
                .contentType(JAVASCRIPT)
                .cacheControl(CacheControl.noStore())
                .body(script);
    }

    private static String resolvePluginTarget(
            final String pluginId,
            final HttpServletRequest request) {

        String requestPath = request.getRequestURI();
        final String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }

        final String pluginPrefix = "/" + pluginId;
        if (!requestPath.startsWith(pluginPrefix)) {
            throw new IllegalArgumentException("Unexpected plugin request path: " + requestPath);
        }

        final String target = requestPath.substring(pluginPrefix.length());
        return target.isEmpty() ? "/" : target;
    }

    private static String readClasspathText(final String resource) throws IOException {
        try (InputStream input = NexuHttpController.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Classpath resource not found: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String trim(final String value) {
        return value == null ? "" : value.trim();
    }

    private static String escapeJson(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
