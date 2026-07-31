package lu.nowina.nexu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import lu.nowina.nexu.api.AppConfig;
import lu.nowina.nexu.api.NexuAPI;

class SystrayBrowserEnableTest {

    @Test
    void usesConfiguredHttpsPortWithLocalhost() {
        final NexuAPI api = mock(NexuAPI.class);
        final AppConfig config = mock(AppConfig.class);
        when(api.getAppConfig()).thenReturn(config);
        when(config.getBindingPortsHttps()).thenReturn(List.of(10443));

        assertEquals("https://localhost:10443/nexu-info", SystrayMenu.browserEnableEndpoint(api));
    }

    @Test
    void fallsBackToTheDefaultHttpsPort() {
        final NexuAPI api = mock(NexuAPI.class);
        final AppConfig config = mock(AppConfig.class);
        when(api.getAppConfig()).thenReturn(config);
        when(config.getBindingPortsHttps()).thenReturn(List.of());

        assertEquals("https://localhost:9895/nexu-info", SystrayMenu.browserEnableEndpoint(api));
    }
}
