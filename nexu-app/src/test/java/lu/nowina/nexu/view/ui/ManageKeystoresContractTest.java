package lu.nowina.nexu.view.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ManageKeystoresContractTest {

    @Test
    void manageDialogSupportsOpeningAndAddingSigningSources() throws Exception {
        final String controller = Files.readString(Path.of(
                "src", "main", "java", "lu", "nowina", "nexu", "view", "ui",
                "ManageKeystoresController.java"));
        final String fxml = Files.readString(Path.of(
                "src", "main", "resources", "fxml", "manage-keystores.fxml"));

        assertTrue(controller.contains("DesktopFileOpener.openWithDefaultApplication"));
        assertTrue(controller.contains("TerminalFactory.getDefault()"));
        assertTrue(controller.contains("database.add(configuredKeystore)"));
        assertTrue(controller.contains("KeystoreType.JKS"));
        assertTrue(controller.contains("KeystoreType.PKCS12"));

        assertTrue(fxml.contains("fx:id=\"addSmartCard\""));
        assertTrue(fxml.contains("fx:id=\"addLocalKeystore\""));
        assertTrue(fxml.contains("fx:id=\"open\""));
        assertTrue(fxml.contains("fx:id=\"remove\""));
    }
}
