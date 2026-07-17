package lu.nowina.nexu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import lu.nowina.nexu.api.SystrayMenuItem;
import lu.nowina.nexu.keystore.KeystoreProductAdapter;

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

        final SystrayMenuItem manageKeystores = new KeystoreProductAdapter(temporaryDirectory.toFile())
                .getExtensionSystrayMenuItem();

        assertNotNull(manageKeystores, "The keystore adapter must contribute a tray-menu item");
        assertFalse(manageKeystores.getLabel().isBlank(), "The keystore tray-menu label must not be blank");
        assertNotNull(manageKeystores.getFutureOperationInvocation(),
                "The keystore tray-menu item must open the management operation");
    }
}
