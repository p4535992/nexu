package lu.nowina.nexu.springboot.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import lu.nowina.nexu.api.AppConfig;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.plugin.HttpPlugin;
import lu.nowina.nexu.api.plugin.HttpRequest;
import lu.nowina.nexu.api.plugin.HttpResponse;
import lu.nowina.nexu.api.plugin.HttpStatus;

class NexuHttpControllerTest {

    private NexuAPI api;
    private AppConfig appConfig;
    private NexuHttpController controller;

    @BeforeEach
    void setUp() {
        api = mock(NexuAPI.class);
        appConfig = mock(AppConfig.class);
        when(api.getAppConfig()).thenReturn(appConfig);
        controller = new NexuHttpController(api);
    }

    @Test
    void exposesLegacyNexuInfoShape() {
        when(appConfig.getApplicationVersion()).thenReturn("1.24-SNAPSHOT\n");

        final ResponseEntity<String> response = controller.nexuInfo();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("{ \"version\": \"1.24-SNAPSHOT\"}", response.getBody());
    }

    @Test
    void delegatesRestCallsToLegacyPluginContract() throws Exception {
        final HttpPlugin plugin = mock(HttpPlugin.class);
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(api.getHttpPlugin("rest")).thenReturn(plugin);
        when(servletRequest.getRequestURI()).thenReturn("/rest/certificates");
        when(servletRequest.getContextPath()).thenReturn("");
        when(plugin.process(org.mockito.ArgumentMatchers.eq(api), org.mockito.ArgumentMatchers.any(HttpRequest.class)))
                .thenReturn(new HttpResponse("{}", "application/json", HttpStatus.OK));

        final ResponseEntity<String> response = controller.plugin("rest", servletRequest);

        final ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(plugin).process(org.mockito.ArgumentMatchers.eq(api), requestCaptor.capture());
        assertEquals("/certificates", requestCaptor.getValue().getTarget());
        assertEquals(200, response.getStatusCode().value());
        assertEquals("{}", response.getBody());
    }
}
