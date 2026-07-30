package lu.nowina.nexu;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.SystrayMenuItem;
import lu.nowina.nexu.api.flow.FutureOperationInvocation;
import lu.nowina.nexu.api.flow.OperationFactory;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.systray.SystrayMenuInitializer;
import lu.nowina.nexu.view.core.NonBlockingUIOperation;

public class SystrayMenu {

    private static final String AWT_BACKEND = "lu.nowina.nexu.systray.AWTSystrayMenuInitializer";
    private static final String DORKBOX_BACKEND = "lu.nowina.nexu.systray.DorkboxSystrayMenuInitializer";
    private static final Logger LOGGER = LoggerFactory.getLogger(SystrayMenu.class.getName());

    public SystrayMenu(OperationFactory operationFactory, NexuAPI api, UserPreferences prefs) {
        final ResourceBundle resources = ResourceBundle.getBundle("bundles/nexu");
        final List<SystrayMenuItem> extensionItems = api.getExtensionSystrayMenuItems();
        final SystrayMenuItem[] items = new SystrayMenuItem[extensionItems.size() + 4];
        items[0] = createAboutSystrayMenuItem(operationFactory, api, resources);
        items[1] = createPreferencesSystrayMenuItem(operationFactory, api, prefs, resources);
        items[2] = createShowLogsSystrayMenuItem(resources);
        items[3] = createLanguageSystrayMenuItem(prefs, resources);
        int index = 4;
        for (SystrayMenuItem item : extensionItems) {
            items[index++] = item;
        }

        final SystrayMenuItem exitItem = createExitSystrayMenuItem(resources);
        final String tooltip = api.getAppConfig().getApplicationName();
        final URL trayIcon = getClass().getResource("/tray-icon.png");
        final String backend = normalizeBackend(System.getProperty(NexuLauncher.SYSTRAY_BACKEND_PROPERTY, "auto"));
        LOGGER.info("Preparing NexU system tray: os={}, backend={}, tooltip='{}', icon={}, menuItems={}",
                api.getEnvironmentInfo().getOs(), backend, tooltip, trayIcon, items.length + 1);

        final boolean initialized = switch (api.getEnvironmentInfo().getOs()) {
        case WINDOWS -> initializeWindowsTray(backend, tooltip, trayIcon, operationFactory, exitItem, items);
        case MACOSX -> initializeBackend("AWT", AWT_BACKEND, tooltip, trayIcon, operationFactory, exitItem, items);
        case LINUX -> initializeBackend("Dorkbox", DORKBOX_BACKEND, tooltip, trayIcon, operationFactory, exitItem, items);
        case NOT_RECOGNIZED -> {
            LOGGER.error("System tray is not supported for the unrecognized operating system");
            yield false;
        }
        };
        if (!initialized) {
            LOGGER.error("All configured NexU system-tray initialization attempts failed: os={}, backend={}",
                    api.getEnvironmentInfo().getOs(), backend);
        }
    }

    private boolean initializeWindowsTray(String backend, String tooltip, URL icon,
            OperationFactory operationFactory, SystrayMenuItem exitItem, SystrayMenuItem[] items) {
        return switch (backend) {
        case "awt" -> initializeBackend("AWT", AWT_BACKEND, tooltip, icon, operationFactory, exitItem, items);
        case "dorkbox" -> initializeBackend("Dorkbox WindowsNotifyIcon", DORKBOX_BACKEND,
                tooltip, icon, operationFactory, exitItem, items);
        default -> {
            final boolean dorkbox = initializeBackend("Dorkbox WindowsNotifyIcon", DORKBOX_BACKEND,
                    tooltip, icon, operationFactory, exitItem, items);
            if (dorkbox) {
                yield true;
            }
            LOGGER.warn("Dorkbox Windows tray initialization failed; trying the AWT fallback");
            yield initializeBackend("AWT fallback", AWT_BACKEND, tooltip, icon,
                    operationFactory, exitItem, items);
        }
        };
    }

    private boolean initializeBackend(String name, String implementation, String tooltip, URL icon,
            OperationFactory operationFactory, SystrayMenuItem exitItem, SystrayMenuItem[] items) {
        try {
            LOGGER.info("Starting NexU tray backend {} ({})", name, implementation);
            final SystrayMenuInitializer initializer = Class.forName(implementation)
                    .asSubclass(SystrayMenuInitializer.class).getDeclaredConstructor().newInstance();
            final boolean initialized = initializer.init(tooltip, icon, operationFactory, exitItem, items);
            if (initialized) {
                LOGGER.info("NexU tray backend {} initialized successfully", name);
            } else {
                LOGGER.warn("NexU tray backend {} reported initialization failure", name);
            }
            return initialized;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            LOGGER.error("Cannot initialize NexU tray backend " + name, exception);
            return false;
        }
    }

    static String normalizeBackend(String backend) {
        if (backend == null) {
            return "auto";
        }
        final String normalized = backend.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
        case "awt", "dorkbox", "auto" -> normalized;
        default -> "auto";
        };
    }

    private SystrayMenuItem createAboutSystrayMenuItem(OperationFactory operationFactory, NexuAPI api,
            ResourceBundle resources) {
        return new SystrayMenuItem() {
            @Override
            public String getLabel() {
                return resources.getString("systray.menu.about");
            }

            @Override
            public FutureOperationInvocation<Void> getFutureOperationInvocation() {
                return factory -> factory.getOperation(NonBlockingUIOperation.class, "/fxml/about.fxml",
                        api.getAppConfig().getApplicationName(), api.getAppConfig().getApplicationVersion(),
                        resources).perform();
            }
        };
    }

    private SystrayMenuItem createPreferencesSystrayMenuItem(OperationFactory operationFactory, NexuAPI api,
            UserPreferences prefs, ResourceBundle resources) {
        return new SystrayMenuItem() {
            @Override
            public String getLabel() {
                return resources.getString("systray.menu.preferences");
            }

            @Override
            public FutureOperationInvocation<Void> getFutureOperationInvocation() {
                return factory -> {
                    final ProxyConfigurer proxyConfigurer = new ProxyConfigurer(api.getAppConfig(), prefs);
                    return factory.getOperation(NonBlockingUIOperation.class, "/fxml/preferences.fxml",
                            proxyConfigurer, prefs, !api.getAppConfig().isUserPreferencesEditable()).perform();
                };
            }
        };
    }

    private SystrayMenuItem createShowLogsSystrayMenuItem(ResourceBundle resources) {
        return new SystrayMenuItem() {
            @Override
            public String getLabel() {
                return resources.getString("systray.menu.show.logs");
            }

            @Override
            public FutureOperationInvocation<Void> getFutureOperationInvocation() {
                return factory -> {
                    Platform.runLater(() -> showLogFileDialog(resources));
                    return new OperationResult<>((Void) null);
                };
            }
        };
    }

    private SystrayMenuItem createLanguageSystrayMenuItem(UserPreferences prefs, ResourceBundle resources) {
        return new SystrayMenuItem() {
            @Override
            public String getLabel() {
                return resources.getString("systray.menu.select.language");
            }

            @Override
            public FutureOperationInvocation<Void> getFutureOperationInvocation() {
                return factory -> {
                    Platform.runLater(() -> showLanguageSelectionDialog(prefs, resources));
                    return new OperationResult<>((Void) null);
                };
            }
        };
    }

    private void showLogFileDialog(ResourceBundle resources) {
        final Path logFile;
        try {
            logFile = prepareLogFile(NexuLogging.currentLogFile());
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Cannot prepare the NexU diagnostic log file", exception);
            showLogOpenError(resources, NexuLogging.currentLogFile());
            return;
        }

        final TextField pathField = new TextField(logFile.toString());
        pathField.setEditable(false);
        pathField.setPrefColumnCount(70);
        final VBox content = new VBox(8,
                new Label(resources.getString("systray.logs.dialog.path")), pathField);
        final ButtonType openButton = new ButtonType(resources.getString("systray.logs.open.button"), ButtonData.OTHER);
        final ButtonType closeButton = new ButtonType(resources.getString("systray.logs.close.button"),
                ButtonData.CANCEL_CLOSE);
        final Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(resources.getString("systray.logs.dialog.title"));
        alert.setHeaderText(resources.getString("systray.logs.dialog.header"));
        alert.getDialogPane().setContent(content);
        alert.getButtonTypes().setAll(openButton, closeButton);
        JavaFxWindowManager.showExclusiveAndWait(alert)
                .filter(openButton::equals)
                .ifPresent(ignored -> openLogFile(logFile, resources));
    }

    static Path prepareLogFile(Path logFile) throws IOException {
        if (logFile == null) {
            throw new IOException("NexU logging has not been configured");
        }
        final Path normalized = logFile.toAbsolutePath().normalize();
        final Path parent = normalized.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (Files.notExists(normalized)) {
            Files.createFile(normalized);
        }
        return normalized;
    }

    private void openLogFile(Path logFile, ResourceBundle resources) {
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new IOException("Desktop integration is not supported");
            }
            final Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.OPEN)) {
                throw new IOException("Desktop open action is not supported");
            }
            desktop.open(logFile.toFile());
            LOGGER.info("Opened NexU diagnostic log file {} with the operating-system default application", logFile);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Cannot open the NexU diagnostic log file {}", logFile, exception);
            showLogOpenError(resources, logFile);
        }
    }

    private void showLogOpenError(ResourceBundle resources, Path logFile) {
        final Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(resources.getString("systray.logs.error.title"));
        alert.setHeaderText(resources.getString("systray.logs.error.header"));
        alert.setContentText(MessageFormat.format(resources.getString("systray.logs.error.content"),
                logFile != null ? logFile.toAbsolutePath().normalize() : "-"));
        JavaFxWindowManager.showExclusiveAndWait(alert);
    }

    private void showLanguageSelectionDialog(UserPreferences prefs, ResourceBundle resources) {
        final Map<String, ApplicationLanguage> languages = new LinkedHashMap<>();
        for (ApplicationLanguage language : ApplicationLanguage.values()) {
            languages.put(resources.getString(language.getLabelKey()), language);
        }
        final String current = resources.getString(prefs.getLanguage().getLabelKey());
        final ChoiceDialog<String> dialog = new ChoiceDialog<>(current, languages.keySet());
        dialog.setTitle(resources.getString("language.selection.title"));
        dialog.setHeaderText(resources.getString("language.selection.header"));
        dialog.setContentText(resources.getString("language.selection.prompt"));
        JavaFxWindowManager.showExclusiveAndWait(dialog).map(languages::get).ifPresent(selected -> {
            if (selected != prefs.getLanguage()) {
                prefs.setLanguage(selected);
                final Alert restart = new Alert(AlertType.INFORMATION);
                restart.setTitle(resources.getString("language.selection.restart.title"));
                restart.setHeaderText(resources.getString("language.selection.restart.header"));
                restart.setContentText(resources.getString("language.selection.restart.content"));
                JavaFxWindowManager.showExclusiveAndWait(restart);
            }
        });
    }

    private SystrayMenuItem createExitSystrayMenuItem(ResourceBundle resources) {
        return new SystrayMenuItem() {
            @Override
            public String getLabel() {
                return resources.getString("systray.menu.exit");
            }

            @Override
            public FutureOperationInvocation<Void> getFutureOperationInvocation() {
                return factory -> {
                    LOGGER.info("Exiting...");
                    Platform.exit();
                    return new OperationResult<>((Void) null);
                };
            }
        };
    }
}
