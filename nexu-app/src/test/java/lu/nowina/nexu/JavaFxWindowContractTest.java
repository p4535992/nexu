package lu.nowina.nexu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class JavaFxWindowContractTest {

    @Test
    void topLevelWindowsAreExclusiveAndUseTheNexuKeyIcon() throws Exception {
        final String managerSource = Files.readString(Path.of(
                "src", "main", "java", "lu", "nowina", "nexu", "JavaFxWindowManager.java"));
        final String displaySource = Files.readString(Path.of(
                "src", "main", "java", "lu", "nowina", "nexu", "StandaloneUIDisplay.java"));
        final String traySource = Files.readString(Path.of(
                "src", "main", "java", "lu", "nowina", "nexu", "SystrayMenu.java"));

        assertTrue(managerSource.contains("Window.getWindows()"),
                "The coordinator must inspect all visible JavaFX stages");
        assertTrue(managerSource.contains("/tray-icon.png"),
                "The JavaFX title-bar icon must reuse the NexU key icon");
        assertTrue(managerSource.contains("stage.getIcons().add(APPLICATION_ICON)"),
                "Every JavaFX Stage must receive the NexU key icon");
        assertTrue(displaySource.contains("JavaFxWindowManager.focusExistingWindow()"),
                "A second FXML view must focus the existing window instead of replacing it");
        assertTrue(displaySource.contains("operation.signalUserCancel()"),
                "A rejected blocking operation must be released instead of waiting forever");
        assertTrue(traySource.contains("JavaFxWindowManager.showExclusiveAndWait"),
                "Tray alerts and choice dialogs must use the same exclusive-window coordinator");
    }
}
