/**
 * © Nowina Solutions, 2015-2017
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package lu.nowina.nexu;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceDialog;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.SystrayMenuItem;
import lu.nowina.nexu.api.flow.FutureOperationInvocation;
import lu.nowina.nexu.api.flow.OperationFactory;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.systray.SystrayMenuInitializer;
import lu.nowina.nexu.view.core.NonBlockingUIOperation;

public class SystrayMenu {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystrayMenu.class.getName());

    public SystrayMenu(OperationFactory operationFactory, NexuAPI api, UserPreferences prefs) {
        final ResourceBundle resources = ResourceBundle.getBundle("bundles/nexu");

        final List<SystrayMenuItem> extensionSystrayMenuItems = api.getExtensionSystrayMenuItems();
        final SystrayMenuItem[] systrayMenuItems = new SystrayMenuItem[extensionSystrayMenuItems.size() + 4];

        systrayMenuItems[0] = createAboutSystrayMenuItem(operationFactory, api, resources);
        systrayMenuItems[1] = createPreferencesSystrayMenuItem(operationFactory, api, prefs, resources);
        systrayMenuItems[2] = createShowLogsSystrayMenuItem(resources);
        systrayMenuItems[3] = createLanguageSystrayMenuItem(prefs, resources);

        int i = 4;
        for (final SystrayMenuItem systrayMenuItem : extensionSystrayMenuItems) {
            systrayMenuItems[i++] = systrayMenuItem;
        }

        final SystrayMenuItem exitMenuItem = createExitSystrayMenuItem(resources);

        final String tooltip = api.getAppConfig().getApplicationName();
        final URL trayIconURL = this.getClass().getResource("/tray-icon.png");
        try {
            switch (api.getEnvironmentInfo().getOs()) {
            case WINDOWS:
            case MACOSX:
                // Use reflection to avoid wrong initialization issues
                Class.forName("lu.nowina.nexu.systray.AWTSystrayMenuInitializer")
                    .asSubclass(SystrayMenuInitializer.class).newInstance()
                    .init(tooltip, trayIconURL, operationFactory, exitMenuItem, systrayMenuItems);
                break;
            case LINUX:
                // Use reflection to avoid wrong initialization issues
                Class.forName("lu.nowina.nexu.systray.DorkboxSystrayMenuInitializer")
                    .asSubclass(SystrayMenuInitializer.class).newInstance()
                    .init(tooltip, trayIconURL, operationFactory, exitMenuItem, systrayMenuItems);
                break;
            case NOT_RECOGNIZED:
                LOGGER.warn("System tray is currently not supported for NOT_RECOGNIZED OS.");
                break;
            default:
                throw new IllegalArgumentException("Unhandled value: " + api.getEnvironmentInfo().getOs());
            }
        } catch (InstantiationException e) {
            LOGGER.error("Cannot initialize systray menu", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("Cannot initialize systray menu", e);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Cannot initialize systray menu", e);
        }
    }

    private SystrayMenuItem createAboutSystrayMenuItem(final OperationFactory operationFactory, final NexuAPI api,
            final ResourceBundle resources) {
        return new SystrayMenuItem() {
            @Override
            public String getLabel() {
                return resources.getString("systray.menu.about");
            }

            @Override
            public FutureOperationInvocation<Void> getFutureOperationInvocation() {
                return new FutureOperationInvocation<Void>() {
                    @Override
                    public OperationResult<Void> call(OperationFactory operationFactory) {
                        return operationFactory.getOperation(NonBlockingUIOperation.class, "/fxml/about.fxml",
                                api.getAppConfig().getApplicationName(), api.getAppConfig().getApplicationVersion(),
                                resources).perform();
                    }
                };
            }
        };
    }

    private SystrayMenuItem createPreferencesSystrayMenuItem(final OperationFactory operationFactory,
            final NexuAPI api, final UserPreferences prefs, final ResourceBundle resources) {
        return new SystrayMenuItem() {
            @Override
            public String getLabel() {
                return resources.getString("systray.menu.preferences");
            }

            @Override
            public FutureOperationInvocation<Void> getFutureOperationInvocation() {
                return new FutureOperationInvocation<Void>() {
                    @Override
                    public OperationResult<Void> call(OperationFactory operationFactory) {
                        final ProxyConfigurer proxyConfigurer = new ProxyConfigurer(api.getAppConfig(), prefs);

                        return operationFactory.getOperation(NonBlockingUIOperation.class, "/fxml/preferences.fxml",
                                proxyConfigurer, prefs, !api.getAppConfig().isUserPreferencesEditable()).perform();
                    }
                };
            }
        };
    }

    private SystrayMenuItem createShowLogsSystrayMenuItem(final ResourceBundle resources) {
        return new SystrayMenuItem() {
            @Override
            public String getLabel() {
                return resources.getString("systray.menu.show.logs");
            }

            @Override
            public FutureOperationInvocation<Void> getFutureOperationInvocation() {
                return operationFactory -> {
                    openLogFile(resources);
                    return new OperationResult<Void>((Void) null);
                };
            }
        };
    }

    private SystrayMenuItem createLanguageSystrayMenuItem(
            final UserPreferences prefs,
            final ResourceBundle resources) {
        return new SystrayMenuItem() {
            @Override
            public String getLabel() {
                return resources.getString("systray.menu.select.language");
            }

            @Override
            public FutureOperationInvocation<Void> getFutureOperationInvocation() {
                return operationFactory -> {
                    Platform.runLater(() -> showLanguageSelectionDialog(prefs, resources));
                    return new OperationResult<Void>((Void) null);
                };
            }
        };
    }

    private void openLogFile(ResourceBundle resources) {
        final Path logFile = NexuLogging.currentLogFile();
        try {
            if (logFile == null) {
                throw new IOException("NexU logging has not been configured");
            }
            Files.createDirectories(logFile.getParent());
            if (Files.notExists(logFile)) {
                Files.createFile(logFile);
            }
            if (!Desktop.isDesktopSupported()) {
                throw new IOException("Desktop integration is not supported");
            }
            final Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.OPEN)) {
                throw new IOException("Desktop open action is not supported");
            }
            desktop.open(logFile.toFile());
            LOGGER.info("Opened NexU diagnostic log file {}", logFile);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Cannot open the NexU diagnostic log file {}", logFile, e);
            Platform.runLater(() -> showLogOpenError(resources, logFile));
        }
    }

    private void showLogOpenError(ResourceBundle resources, Path logFile) {
        final Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(resources.getString("systray.logs.error.title"));
        alert.setHeaderText(resources.getString("systray.logs.error.header"));
        alert.setContentText(MessageFormat.format(
                resources.getString("systray.logs.error.content"),
                logFile != null ? logFile.toAbsolutePath().normalize() : "-"));
        alert.showAndWait();
    }

    private void showLanguageSelectionDialog(UserPreferences prefs, ResourceBundle resources) {
        final Map<String, ApplicationLanguage> languages = new LinkedHashMap<>();
        for (ApplicationLanguage language : ApplicationLanguage.values()) {
            languages.put(resources.getString(language.getLabelKey()), language);
        }

        final String currentLanguageLabel = resources.getString(prefs.getLanguage().getLabelKey());
        final ChoiceDialog<String> dialog = new ChoiceDialog<>(currentLanguageLabel, languages.keySet());
        dialog.setTitle(resources.getString("language.selection.title"));
        dialog.setHeaderText(resources.getString("language.selection.header"));
        dialog.setContentText(resources.getString("language.selection.prompt"));
        dialog.showAndWait().map(languages::get).ifPresent(selectedLanguage -> {
            if (selectedLanguage != prefs.getLanguage()) {
                prefs.setLanguage(selectedLanguage);
                final Alert restartAlert = new Alert(AlertType.INFORMATION);
                restartAlert.setTitle(resources.getString("language.selection.restart.title"));
                restartAlert.setHeaderText(resources.getString("language.selection.restart.header"));
                restartAlert.setContentText(resources.getString("language.selection.restart.content"));
                restartAlert.showAndWait();
            }
        });
    }

    private SystrayMenuItem createExitSystrayMenuItem(final ResourceBundle resources) {
        return new SystrayMenuItem() {
            @Override
            public String getLabel() {
                return resources.getString("systray.menu.exit");
            }

            @Override
            public FutureOperationInvocation<Void> getFutureOperationInvocation() {
                return new FutureOperationInvocation<Void>() {
                    @Override
                    public OperationResult<Void> call(OperationFactory operationFactory) {
                        LOGGER.info("Exiting...");
                        Platform.exit();
                        return new OperationResult<Void>((Void) null);
                    }
                };
            }
        };
    }
}
