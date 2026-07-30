package lu.nowina.nexu;

import java.net.URL;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Coordinates NexU top-level JavaFX windows.
 *
 * <p>Only one independent NexU window is allowed to be visible at a time. A
 * second tray action focuses the existing window instead of replacing its
 * scene or opening another stage. The same coordinator also applies the NexU
 * key icon to every JavaFX {@link Stage}, including stages created internally
 * for alerts and choice dialogs.</p>
 */
final class JavaFxWindowManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaFxWindowManager.class);
    private static final URL APPLICATION_ICON_URL = JavaFxWindowManager.class.getResource("/tray-icon.png");
    private static final Image APPLICATION_ICON = loadApplicationIcon();

    private static boolean iconListenerInstalled;

    private JavaFxWindowManager() {
        // Utility class.
    }

    static void installApplicationIconSupport() {
        if (Platform.isFxApplicationThread()) {
            installApplicationIconSupportOnFxThread();
        } else {
            Platform.runLater(JavaFxWindowManager::installApplicationIconSupportOnFxThread);
        }
    }

    private static synchronized void installApplicationIconSupportOnFxThread() {
        if (iconListenerInstalled) {
            return;
        }

        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(JavaFxWindowManager::applyApplicationIcon);
                }
            }
        });
        Window.getWindows().forEach(JavaFxWindowManager::applyApplicationIcon);
        iconListenerInstalled = true;
        LOGGER.info("Installed NexU JavaFX stage-icon support using {}", APPLICATION_ICON_URL);
    }

    static void applyApplicationIcon(Window window) {
        if (APPLICATION_ICON == null || !(window instanceof Stage stage)) {
            return;
        }
        if (!stage.getIcons().contains(APPLICATION_ICON)) {
            stage.getIcons().add(APPLICATION_ICON);
        }
    }

    /**
     * Focuses the currently visible NexU stage, if any.
     *
     * @return {@code true} when a visible stage was found and focused
     */
    static boolean focusExistingWindow() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("JavaFX windows must be inspected on the JavaFX application thread");
        }

        final Optional<Stage> existingStage = Window.getWindows().stream()
                .filter(Window::isShowing)
                .filter(Stage.class::isInstance)
                .map(Stage.class::cast)
                .findFirst();

        if (existingStage.isEmpty()) {
            return false;
        }

        final Stage stage = existingStage.get();
        if (stage.isIconified()) {
            stage.setIconified(false);
        }
        stage.toFront();
        stage.requestFocus();
        LOGGER.info("A NexU JavaFX window is already open; focused it instead of opening a second window");
        return true;
    }

    static <R> Optional<R> showExclusiveAndWait(Dialog<R> dialog) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("JavaFX dialogs must be shown on the JavaFX application thread");
        }
        if (focusExistingWindow()) {
            return Optional.empty();
        }

        dialog.addEventHandler(DialogEvent.DIALOG_SHOWN,
                event -> applyApplicationIcon(dialog.getDialogPane().getScene().getWindow()));
        return dialog.showAndWait();
    }

    private static Image loadApplicationIcon() {
        if (APPLICATION_ICON_URL == null) {
            LOGGER.warn("NexU JavaFX application icon /tray-icon.png is missing");
            return null;
        }
        try {
            return new Image(APPLICATION_ICON_URL.toExternalForm(), false);
        } catch (RuntimeException exception) {
            LOGGER.warn("Cannot load NexU JavaFX application icon {}", APPLICATION_ICON_URL, exception);
            return null;
        }
    }
}
