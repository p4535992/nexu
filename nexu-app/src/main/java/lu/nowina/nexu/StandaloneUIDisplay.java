package lu.nowina.nexu;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.token.PasswordInputCallback;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lu.nowina.nexu.api.AppConfig;
import lu.nowina.nexu.api.MessageDisplayCallback;
import lu.nowina.nexu.api.NexuPasswordInputCallback;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.flow.OperationFactory;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.view.core.ExtensionFilter;
import lu.nowina.nexu.view.core.NonBlockingUIOperation;
import lu.nowina.nexu.view.core.UIDisplay;
import lu.nowina.nexu.view.core.UIOperation;
import lu.nowina.nexu.view.ui.support.UIOperationController;

/** Implementation of {@link UIDisplay} used for standalone mode. */
public class StandaloneUIDisplay implements UIDisplay {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandaloneUIDisplay.class.getName());

    private Stage blockingStage;
    private Stage nonBlockingStage;
    private UIOperation<?> currentBlockingOperation;
    private OperationFactory operationFactory;
    private AppConfig appConfig;
    private char[] cachedPassword;
    private Date cacheLastAccessTime = new Date();

    public StandaloneUIDisplay() {
        JavaFxWindowManager.installApplicationIconSupport();
        this.blockingStage = createStage(true, null);
        this.nonBlockingStage = createStage(false, null);
    }

    public StandaloneUIDisplay(final AppConfig config) {
        this();
        this.appConfig = config;
        LOGGER.info("Using cache_time_to_live_ms = {}", config.getCacheTimeToLiveMs());
    }

    private void display(final Parent panel, final boolean blockingOperation, final UIOperation<?> operation) {
        LOGGER.info("Display {} in display {} from Thread {}", panel, this, Thread.currentThread().getName());
        Platform.runLater(() -> {
            if (JavaFxWindowManager.focusExistingWindow()) {
                LOGGER.info("Skipped JavaFX view {} because another NexU window is already visible",
                        operation != null ? operation.getViewResource() : panel);
                if (blockingOperation && operation != null) {
                    operation.signalUserCancel();
                }
                return;
            }

            Stage stage = blockingOperation ? blockingStage : nonBlockingStage;
            if (!stage.isShowing()) {
                if (blockingOperation) {
                    stage = blockingStage = createStage(true, null);
                } else {
                    stage = nonBlockingStage = createStage(false, null);
                }
                LOGGER.info("Loading ui {} in new Stage {}", panel, stage);
            }

            final Scene scene = new Scene(panel);
            scene.getStylesheets().add(getClass().getResource("/styles/nexu.css").toString());
            stage.setScene(scene);
            stage.setTitle(StageHelper.getInstance().getTitle());
            stage.show();
            StageHelper.getInstance().setTitle("", null);
        });
    }

    private Stage createStage(final boolean blockingStage, final String title) {
        final Stage newStage = new Stage();
        JavaFxWindowManager.applyApplicationIcon(newStage);
        newStage.setTitle(title);
        newStage.setAlwaysOnTop(true);
        newStage.setOnCloseRequest(event -> {
            LOGGER.info("Closing stage {} from {}", newStage, Thread.currentThread().getName());
            newStage.hide();
            event.consume();
            if (blockingStage && currentBlockingOperation != null) {
                currentBlockingOperation.signalUserCancel();
            }
        });
        return newStage;
    }

    @Override
    public void close(final boolean blockingOperation) {
        Platform.runLater(() -> {
            final Stage oldStage = blockingOperation ? blockingStage : nonBlockingStage;
            LOGGER.info("Hide stage {} and create new stage", oldStage);
            if (blockingOperation) {
                blockingStage = createStage(true, null);
            } else {
                nonBlockingStage = createStage(false, null);
            }
            oldStage.hide();
        });
    }

    @Override
    public <T> void displayAndWaitUIOperation(final UIOperation<T> operation) {
        display(loadView(operation), true, operation);
        waitForUser(operation);
    }

    private <T> Parent loadView(final UIOperation<T> operation) {
        final FXMLLoader loader = new FXMLLoader();
        loader.setResources(ResourceBundle.getBundle("bundles/nexu"));
        try {
            loader.load(getClass().getResourceAsStream(operation.getViewResource()));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load UI resource " + operation.getViewResource(), exception);
        }

        @SuppressWarnings("unchecked")
        final UIOperationController<T> controller = loader.getController();
        if (controller == null) {
            throw new IllegalStateException("No controller declared for " + operation.getViewResource());
        }
        controller.init(operation.getControllerParams());
        controller.setUIOperation(operation);
        controller.setDisplay(this);
        return loader.getRoot();
    }

    private <T> void waitForUser(final UIOperation<T> operation) {
        try {
            LOGGER.info("Wait on Thread {}", Thread.currentThread().getName());
            currentBlockingOperation = operation;
            operation.waitEnd();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        } finally {
            currentBlockingOperation = null;
        }
    }

    private final class FlowPasswordCallback implements NexuPasswordInputCallback {

        private String passwordPrompt;
        private char[] cachedPassword;
        private StandaloneUIDisplay parent;
        private int passwordRequestIndex;

        FlowPasswordCallback() {
            this.passwordPrompt = null;
            this.passwordRequestIndex = 0;
        }

        FlowPasswordCallback(final StandaloneUIDisplay parent, final char[] cachedPassword) {
            this();
            this.parent = parent;
            this.cachedPassword = cachedPassword;
        }

        @Override
        public char[] getPassword() {
            final PasswordPromptMessages.Prompt prompt = PasswordPromptMessages.resolve(
                    NexuLauncher.getConfig().getCloseToken(), passwordRequestIndex++, passwordPrompt);

            if (cachedPassword != null) {
                LOGGER.info("Returning cached password");
                final char[] clone = cachedPassword.clone();
                cachedPassword = null;
                return clone;
            }

            LOGGER.info("Request password for stage {}", passwordRequestIndex);
            @SuppressWarnings("unchecked")
            final OperationResult<char[]> passwordResult = StandaloneUIDisplay.this.operationFactory.getOperation(
                    UIOperation.class,
                    "/fxml/password-input.fxml",
                    prompt.message(),
                    NexuLauncher.getConfig().getApplicationName(),
                    prompt.title()).perform();
            if (passwordResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
                parent.setCachedPassword(passwordResult.getResult());
                return passwordResult.getResult();
            }
            parent.setCachedPassword(null);
            if (passwordResult.getStatus().equals(BasicOperationStatus.USER_CANCEL)) {
                throw new CancelledOperationException();
            }
            if (passwordResult.getStatus().equals(BasicOperationStatus.EXCEPTION)) {
                final Exception exception = passwordResult.getException();
                if (exception instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new NexuException(exception);
            }
            throw new IllegalArgumentException(
                    "Not managed operation status: " + passwordResult.getStatus().getCode());
        }

        @Override
        public void setPasswordPrompt(final String passwordPrompt) {
            this.passwordPrompt = passwordPrompt;
        }
    }

    @Override
    public PasswordInputCallback getPasswordInputCallback() {
        return new FlowPasswordCallback(this, getCachedPassword());
    }

    private final class FlowMessageDisplayCallback implements MessageDisplayCallback {
        @Override
        public void display(final Message message) {
            if (Message.INPUT_PINPAD.equals(message)) {
                StandaloneUIDisplay.this.operationFactory.getOperation(
                        NonBlockingUIOperation.class,
                        "/fxml/message-no-button.fxml",
                        "message.display.callback." + message.name().toLowerCase().replace('_', '.'),
                        NexuLauncher.getConfig().getApplicationName()).perform();
            } else {
                StandaloneUIDisplay.this.operationFactory.getOperation(
                        NonBlockingUIOperation.class,
                        "/fxml/message.fxml",
                        "message.display.callback." + message.name().toLowerCase().replace('_', '.'),
                        NexuLauncher.getConfig().getApplicationName()).perform();
            }
        }

        @Override
        public void dispose() {
            StandaloneUIDisplay.this.close(false);
        }
    }

    @Override
    public MessageDisplayCallback getMessageDisplayCallback() {
        return new FlowMessageDisplayCallback();
    }

    @Override
    public File displayFileChooser(final ExtensionFilter... extensionFilters) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(ResourceBundle.getBundle("bundles/nexu")
                .getString("fileChooser.title.openResourceFile"));
        fileChooser.getExtensionFilters().addAll(toJavaFXExtensionFilters(extensionFilters));
        return fileChooser.showOpenDialog(blockingStage);
    }

    private javafx.stage.FileChooser.ExtensionFilter[] toJavaFXExtensionFilters(
            final ExtensionFilter... extensionFilters) {
        final javafx.stage.FileChooser.ExtensionFilter[] result =
                new javafx.stage.FileChooser.ExtensionFilter[extensionFilters.length];
        int index = 0;
        for (final ExtensionFilter extensionFilter : extensionFilters) {
            result[index++] = new javafx.stage.FileChooser.ExtensionFilter(
                    extensionFilter.getDescription(), extensionFilter.getExtensions());
        }
        return result;
    }

    public void setOperationFactory(final OperationFactory operationFactory) {
        this.operationFactory = operationFactory;
    }

    @Override
    public void display(final NonBlockingUIOperation operation) {
        display(loadView(operation), false, operation);
    }

    @Override
    public void setCachedPassword(final char[] value) {
        if (value != null) {
            cacheLastAccessTime = new Date();
        }
        this.cachedPassword = value;
    }

    private char[] getCachedPassword() {
        if (cachedPassword != null) {
            final Date now = new Date();
            if (now.getTime() - cacheLastAccessTime.getTime() > appConfig.getCacheTimeToLiveMs()) {
                LOGGER.info("Cache is stale, will request password");
                cachedPassword = null;
            } else {
                cacheLastAccessTime = now;
            }
        }
        return cachedPassword;
    }
}
