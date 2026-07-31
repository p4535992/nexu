package lu.nowina.nexu.view.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

class BrowserEnableControllerContractTest {

    @Test
    void panelExplainsTheLocalCertificateFlowInBothLanguages() throws Exception {
        assertEquals("https://localhost:9895/nexu-info", BrowserEnableController.DEFAULT_ENDPOINT);

        final String fxml = Files.readString(
                Path.of("src", "main", "resources", "fxml", "browser-enable.fxml"));
        assertTrue(fxml.contains("fx:controller=\"lu.nowina.nexu.view.ui.BrowserEnableController\""));
        assertTrue(fxml.contains("fx:id=\"endpoint\""));
        assertTrue(fxml.contains("fx:id=\"openBrowser\""));
        assertTrue(fxml.contains("fx:id=\"status\""));

        final ResourceBundle english = ResourceBundle.getBundle("bundles/browser-enable", Locale.ENGLISH);
        final ResourceBundle italian = ResourceBundle.getBundle("bundles/browser-enable", Locale.ITALIAN);

        assertEquals("Enable NexU in browser", english.getString("browser.enable.menu"));
        assertEquals("Abilita NexU nel browser", italian.getString("browser.enable.menu"));
        assertTrue(english.getString("browser.enable.instructions").contains("standard advanced or continue"));
        assertTrue(english.getString("browser.enable.security").contains("config/localhost.crt"));
        assertTrue(italian.getString("browser.enable.instructions").contains("procedura standard del browser"));
        assertTrue(italian.getString("browser.enable.security").contains("config/localhost.crt"));
    }
}
