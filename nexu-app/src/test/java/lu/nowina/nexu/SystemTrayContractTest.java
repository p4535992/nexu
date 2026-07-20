package lu.nowina.nexu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import lu.nowina.nexu.api.AppConfig;
import lu.nowina.nexu.api.SystrayMenuItem;
import lu.nowina.nexu.keystore.KeystorePlugin;

class SystemTrayContractTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void packagedApplicationKeepsTheOriginalTrayAndKeystoreWorkflow() throws Exception {
        final Properties properties = new Properties();
        try (InputStream input = NexUApp.class.getResourceAsStream("/nexu-config.properties")) {
            assertNotNull(input, "Bundled NexU configuration is missing");
            properties.load(input);
        }

        assertEquals("true", properties.getProperty("enable_systray_menu"),
                "The operator system tray must be explicitly enabled");
        assertNotNull(NexUApp.class.getResource("/tray-icon.png"),
                "The Windows notification-area icon is missing");

        final String runtimeModules = Files.readString(Path.of("src", "jpackage", "modules.txt"));
        assertTrue(runtimeModules.contains("java.desktop"),
                "The jpackage runtime must include java.desktop for AWT SystemTray support");

        final String keystorePluginClass = properties.getProperty("plugin_keystore");
        assertEquals(KeystorePlugin.class.getName(), keystorePluginClass,
                "The bundled configuration must load the keystore plugin");

        final AppConfig appConfig = new AppConfig() {
            @Override
            public File getNexuHome() {
                return temporaryDirectory.toFile();
            }
        };
        appConfig.setConnectionsCacheMaxSize(50);

        final InternalAPI api = new InternalAPI(null, null, null, null, null, appConfig);
        final Properties pluginProperties = new Properties();
        pluginProperties.setProperty("plugin_keystore", keystorePluginClass);
        new APIBuilder().initPlugins(api, pluginProperties);

        final List<SystrayMenuItem> extensionMenuItems = api.getExtensionSystrayMenuItems();
        assertEquals(1, extensionMenuItems.size(),
                "Loading the configured keystore plugin must contribute one tray-menu item");

        final SystrayMenuItem manageKeystores = extensionMenuItems.get(0);
        assertFalse(manageKeystores.getLabel().isBlank(), "The keystore tray-menu label must not be blank");
        assertNotNull(manageKeystores.getFutureOperationInvocation(),
                "The keystore tray-menu item must open the management operation");
    }
}
