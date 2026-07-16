/**
 * © Nowina Solutions, 2015-2016
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

import java.io.File;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.token.PasswordInputCallback;
import java.util.Date;
import javafx.application.Platform;
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

// Unisystems change: added cachedPassword + logic
/**
 * Implementation of {@link UIDisplay} used for standalone mode.
 *
 * @author Jean Lepropre (jean.lepropre@nowina.lu)
 */
public class StandaloneUIDisplay implements UIDisplay {

	private static final Logger LOGGER = LoggerFactory.getLogger(StandaloneUIDisplay.class.getName());

	private Stage blockingStage;
	private Stage nonBlockingStage;
	private UIOperation<?> currentBlockingOperation;
	private OperationFactory operationFactory;
	private AppConfig appConfig;
    private char[] cachedPassword = null;
    private Date cacheLastAccessTime = new Date();
    private static final long CACHE_TIME_TO_LIVE_MS = 5000;
	
	public StandaloneUIDisplay() {
		this.blockingStage = createStage(true, null);
		this.nonBlockingStage = createStage(false, null);
	}
    
    public StandaloneUIDisplay(AppConfig config) {
        this();
        this.appConfig = config;
        LOGGER.info("Using cache_time_to_live_ms = " + config.getCacheTimeToLiveMs());
    }

	private void display(Parent panel, boolean blockingOperation) {
		LOGGER.info("Display " + panel + " in display " + this + " from Thread " + Thread.currentThread().getName());
		Platform.runLater(() -> {
			Stage stage = (blockingOperation) ? blockingStage : nonBlockingStage;
			LOGGER.info("Display " + panel + " in display " + this + " from Thread " + Thread.currentThread().getName());
			if (!stage.isShowing()) {
				if(blockingOperation) {
					stage = blockingStage = createStage(true, null);
				} else {
					stage = nonBlockingStage = createStage(false, null);
				}
				LOGGER.info("Loading ui " + panel + " is a new Stage " + stage);
			} else {
				LOGGER.info("Stage still showing, display " + panel);
			}
			final Scene scene = new Scene(panel);
			scene.getStylesheets().add(this.getClass().getResource("/styles/nexu.css").toString());
			stage.setScene(scene);
			stage.setTitle(StageHelper.getInstance().getTitle());
			stage.show();
			StageHelper.getInstance().setTitle("", null);
		});
	}

	private Stage createStage(final boolean blockingStage, String title) {
		final Stage newStage = new Stage();
		newStage.setTitle(title);
		newStage.setAlwaysOnTop(true);
		newStage.setOnCloseRequest((e) -> {
			LOGGER.info("Closing stage " + newStage + " from " + Thread.currentThread().getName());
			newStage.hide();
			e.consume();

			if (blockingStage && (currentBlockingOperation != null)) {
				currentBlockingOperation.signalUserCancel();
			}
		});
		return newStage;
	}

	@Override
	public void close(final boolean blockingOperation) {
		Platform.runLater(() -> {
			Stage oldStage = (blockingOperation) ? blockingStage : nonBlockingStage;
			LOGGER.info("Hide stage " + oldStage + " and create new stage");
			if(blockingOperation) {
				blockingStage = createStage(true, null);
			} else {
				nonBlockingStage = createStage(false, null);
			}
			oldStage.hide();
		});
	}

	public <T> void displayAndWaitUIOperation(final UIOperation<T> operation) {
		display(operation.getRoot(), true);
		waitForUser(operation);
	}

	private <T> void waitForUser(UIOperation<T> operation) {
		try {
			LOGGER.info("Wait on Thread " + Thread.currentThread().getName());
			currentBlockingOperation = operation;
			operation.waitEnd();
			currentBlockingOperation = null;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private final class FlowPasswordCallback implements NexuPasswordInputCallback {
		
		private String passwordPrompt;
		private char[] cachedPassword = null;
        private StandaloneUIDisplay parent = null;
                
		public FlowPasswordCallback() {
			this.passwordPrompt = null;
		}
                
        public FlowPasswordCallback(StandaloneUIDisplay parent, char[] cachedPassword) {
            this.parent = parent;
            this.cachedPassword = cachedPassword;
        }

		@Override
		public char[] getPassword() {
            if (cachedPassword != null) {
                LOGGER.info("Returning cached password");
                char[] clone = cachedPassword.clone();
                cachedPassword = null; // should need it only once per FlowPasswordCallback object, real cachedPassword saved in StandaloneUIDisplay, but check CACHE_TIME_TO_LIVE_MS
                return clone;
            }
                    
			LOGGER.info("Request password");
			@SuppressWarnings("unchecked")
			final OperationResult<char[]> passwordResult = StandaloneUIDisplay.this.operationFactory.getOperation(
					UIOperation.class, "/fxml/password-input.fxml", passwordPrompt, NexuLauncher.getConfig().getApplicationName()).perform();
			if(passwordResult.getStatus().equals(BasicOperationStatus.SUCCESS)) {
                parent.setCachedPassword(passwordResult.getResult());
				return passwordResult.getResult();
			} 
            else {
                parent.setCachedPassword(null);
                if (passwordResult.getStatus().equals(BasicOperationStatus.USER_CANCEL)) {
                    throw new CancelledOperationException();
                } else if (passwordResult.getStatus().equals(BasicOperationStatus.EXCEPTION)) {
                    final Exception e = passwordResult.getException();
                    if (e instanceof RuntimeException) {
                        // Throw exception as is
                        throw (RuntimeException) e;
                    } else {
                        // Wrap in a runtime exception
                        throw new NexuException(e);
                    }
                } else {
                    throw new IllegalArgumentException("Not managed operation status: " + passwordResult.getStatus().getCode());
                }
            }
		}

		@Override
		public void setPasswordPrompt(String passwordPrompt) {
			this.passwordPrompt = passwordPrompt;
		}
	}

	@Override
	public PasswordInputCallback getPasswordInputCallback() {
		return new FlowPasswordCallback(this, getCachedPassword());
	}
	
	private final class FlowMessageDisplayCallback implements MessageDisplayCallback {
		@Override
		public void display(Message message) {
			if(Message.INPUT_PINPAD.equals(message)) {
				StandaloneUIDisplay.this.operationFactory.getOperation(
						NonBlockingUIOperation.class, "/fxml/message-no-button.fxml",
						"message.display.callback." + message.name().toLowerCase().replace('_', '.'),
						NexuLauncher.getConfig().getApplicationName()).perform();
			} else {
				StandaloneUIDisplay.this.operationFactory.getOperation(
					NonBlockingUIOperation.class, "/fxml/message.fxml",
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
	public File displayFileChooser(ExtensionFilter... extensionFilters) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(ResourceBundle.getBundle("bundles/nexu").getString("fileChooser.title.openResourceFile"));
		fileChooser.getExtensionFilters().addAll(toJavaFXExtensionFilters(extensionFilters));
		return fileChooser.showOpenDialog(blockingStage);
	}
	
	private javafx.stage.FileChooser.ExtensionFilter[] toJavaFXExtensionFilters(ExtensionFilter... extensionFilters) {
		final javafx.stage.FileChooser.ExtensionFilter[] result = new javafx.stage.FileChooser.ExtensionFilter[extensionFilters.length];
		int i = 0;
		for(final ExtensionFilter extensionFilter : extensionFilters) {
			result[i++] = new javafx.stage.FileChooser.ExtensionFilter(extensionFilter.getDescription(), extensionFilter.getExtensions());
		}
		return result;
	}
	
	public void setOperationFactory(final OperationFactory operationFactory) {
		this.operationFactory = operationFactory;
	}

	@Override
	public void display(NonBlockingUIOperation operation) {
		display(operation.getRoot(), false);
	}

    @Override
    public void setCachedPassword(char[] value) {
        if (value != null) {
            cacheLastAccessTime = new Date();
        }
        this.cachedPassword = value;
    }
    
    private char[] getCachedPassword() {
        if (cachedPassword != null) {
            Date now = new Date();
            if (now.getTime() - cacheLastAccessTime.getTime() > appConfig.getCacheTimeToLiveMs()) {
                LOGGER.info("Cache is stale, will request password");
                cachedPassword = null;
            } else {
                cacheLastAccessTime = now;
            }
        }
        
        return cachedPassword;
    }
    
    private boolean hasCachedPassword() {
        Date now = new Date();
        return this.cachedPassword != null && now.getTime() - cacheLastAccessTime.getTime() < appConfig.getCacheTimeToLiveMs();
    }
}
