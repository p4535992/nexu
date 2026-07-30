package lu.nowina.nexu.view.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

class AboutControllerContractTest {

    @Test
    void aboutPanelExposesProjectAndEuplLinksInBothLanguages() throws Exception {
        assertEquals("https://github.com/p4535992/nexu", AboutController.PROJECT_URL);
        assertEquals("https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12",
                AboutController.LICENSE_URL);

        final String fxml = Files.readString(Path.of("src", "main", "resources", "fxml", "about.fxml"));
        assertTrue(fxml.contains("onAction=\"#openProject\""));
        assertTrue(fxml.contains("onAction=\"#openLicense\""));

        final ResourceBundle english = ResourceBundle.getBundle("bundles/nexu", Locale.ENGLISH);
        final ResourceBundle italian = ResourceBundle.getBundle("bundles/nexu", Locale.ITALIAN);
        assertEquals("GitHub repository", english.getString("about.project.link"));
        assertEquals("European Union Public Licence 1.2", english.getString("about.license.link"));
        assertEquals("Repository GitHub", italian.getString("about.project.link"));
        assertEquals("Licenza pubblica dell'Unione europea 1.2", italian.getString("about.license.link"));
    }
}
