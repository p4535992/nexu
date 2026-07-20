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
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.UUID;

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
        final Properties properties = loadProperties("/nexu-config.properties");

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

    @Test
    void trayKeepsAllOriginalActionsAndAddsLogsAndLanguageInBothLocales() throws Exception {
        final ResourceBundle english = ResourceBundle.getBundle("bundles/nexu", Locale.ENGLISH);
        final ResourceBundle italian = ResourceBundle.getBundle("bundles/nexu", Locale.ITALIAN);

        assertEquals("About", english.getString("systray.menu.about"));
        assertEquals("Preferences", english.getString("systray.menu.preferences"));
        assertEquals("Show logs", english.getString("systray.menu.show.logs"));
        assertEquals("Select language", english.getString("systray.menu.select.language"));
        assertEquals("Manage keystores", english.getString("systray.menu.manage.keystores"));
        assertEquals("Exit", english.getString("systray.menu.exit"));

        assertEquals("Informazioni", italian.getString("systray.menu.about"));
        assertEquals("Preferenze", italian.getString("systray.menu.preferences"));
        assertEquals("Mostra log", italian.getString("systray.menu.show.logs"));
        assertEquals("Seleziona lingua", italian.getString("systray.menu.select.language"));
        assertEquals("Gestisci keystore", italian.getString("systray.menu.manage.keystores"));
        assertEquals("Esci", italian.getString("systray.menu.exit"));

        final Properties englishProperties = loadProperties("/bundles/nexu.properties");
        final Properties italianProperties = loadProperties("/bundles/nexu_it.properties");
        assertEquals(englishProperties.stringPropertyNames(), italianProperties.stringPropertyNames(),
                "The Italian bundle must translate every desktop resource key");
    }

    @Test
    void languageStartsInEnglishAndPersistsItalianSelection() {
        final String preferenceNode = "NexU-test-" + UUID.randomUUID();
        final UserPreferences preferences = new UserPreferences(preferenceNode);
        try {
            preferences.clear();
            assertEquals(ApplicationLanguage.ENGLISH, preferences.getLanguage(),
                    "English must be the first-run language regardless of the operating-system locale");

            preferences.setLanguage(ApplicationLanguage.ITALIAN);
            assertEquals(ApplicationLanguage.ITALIAN, new UserPreferences(preferenceNode).getLanguage(),
                    "The selected language must survive an application restart");
        } finally {
            new UserPreferences(preferenceNode).clear();
        }
    }

    @Test
    void showLogsTargetsTheActiveNexuLogFile() {
        final String previousLogDirectory = System.getProperty(NexuLogging.LOG_DIRECTORY_ENVIRONMENT);
        try {
            System.setProperty(NexuLogging.LOG_DIRECTORY_ENVIRONMENT, temporaryDirectory.toString());
            assertEquals(temporaryDirectory.resolve(NexuLogging.LOG_FILE_NAME).toAbsolutePath().normalize(),
                    NexuLogging.currentLogFile());
        } finally {
            if (previousLogDirectory == null) {
                System.clearProperty(NexuLogging.LOG_DIRECTORY_ENVIRONMENT);
            } else {
                System.setProperty(NexuLogging.LOG_DIRECTORY_ENVIRONMENT, previousLogDirectory);
            }
        }
    }

    private static Properties loadProperties(String resourcePath) throws Exception {
        final Properties properties = new Properties();
        try (InputStream input = NexUApp.class.getResourceAsStream(resourcePath)) {
            assertNotNull(input, "Missing classpath resource " + resourcePath);
            properties.load(input);
        }
        return properties;
    }
}
