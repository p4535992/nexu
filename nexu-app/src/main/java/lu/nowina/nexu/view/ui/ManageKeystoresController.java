package lu.nowina.nexu.view.ui;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import lu.nowina.nexu.DesktopFileOpener;
import lu.nowina.nexu.api.ConfiguredKeystore;
import lu.nowina.nexu.api.DetectedCard;
import lu.nowina.nexu.api.KeystoreType;
import lu.nowina.nexu.keystore.KeystoreDatabase;
import lu.nowina.nexu.view.core.ExtensionFilter;
import lu.nowina.nexu.view.ui.support.AbstractUIOperationController;

/**
 * Manages saved local keystore files and provides an explicit PC/SC discovery
 * action for smart cards.
 */
public class ManageKeystoresController extends AbstractUIOperationController<Void> implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManageKeystoresController.class);

    @FXML
    private Button remove;

    @FXML
    private Button open;

    @FXML
    private Button addLocalKeystore;

    @FXML
    private Button addSmartCard;

    @FXML
    private TableView<ConfiguredKeystore> keystoresTable;

    @FXML
    private TableColumn<ConfiguredKeystore, String> keystoreNameTableColumn;

    @FXML
    private TableColumn<ConfiguredKeystore, KeystoreType> keystoreTypeTableColumn;

    @FXML
    private Label keystoreURL;

    @FXML
    private Label status;

    private final ObservableList<ConfiguredKeystore> observableKeystores;
    private KeystoreDatabase database;
    private ResourceBundle resources;

    public ManageKeystoresController() {
        observableKeystores = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.resources = resources;
        keystoresTable.setPlaceholder(new Label(resources.getString("table.view.no.content")));
        keystoresTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        keystoreNameTableColumn.setCellValueFactory(param -> {
            final String url = param.getValue().getUrl();
            return new ReadOnlyStringWrapper(url.substring(url.lastIndexOf('/') + 1));
        });
        keystoreTypeTableColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        keystoresTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            keystoreURL.setText(newValue != null ? newValue.getUrl() : null);
            status.setText("");
        });
        keystoresTable.setItems(observableKeystores);

        remove.disableProperty().bind(keystoresTable.getSelectionModel().selectedItemProperty().isNull());
        open.disableProperty().bind(keystoresTable.getSelectionModel().selectedItemProperty().isNull());

        remove.setOnAction(event -> removeSelectedKeystore());
        open.setOnAction(event -> openSelectedKeystore());
        addLocalKeystore.setOnAction(event -> addLocalKeystore());
        addSmartCard.setOnAction(event -> discoverSmartCards());

        observableKeystores.addListener((ListChangeListener<ConfiguredKeystore>) change -> {
            while (change.next()) {
                for (ConfiguredKeystore removedKeystore : change.getRemoved()) {
                    database.remove(removedKeystore);
                }
            }
        });
    }

    @Override
    public void init(Object... params) {
        database = (KeystoreDatabase) params[0];
        Platform.runLater(() -> observableKeystores.setAll(database.getKeystores()));
    }

    private void removeSelectedKeystore() {
        final ConfiguredKeystore selected = keystoresTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            observableKeystores.remove(selected);
            status.setText(resources.getString("manage.keystores.remove.success"));
        }
    }

    private void openSelectedKeystore() {
        final ConfiguredKeystore selected = keystoresTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            final Path file = Path.of(URI.create(selected.getUrl())).toAbsolutePath().normalize();
            DesktopFileOpener.openWithDefaultApplication(file);
            status.setText(MessageFormat.format(resources.getString("manage.keystores.open.success"), file));
        } catch (Exception e) {
            LOGGER.error("Cannot open registered keystore {}", selected.getUrl(), e);
            status.setText(MessageFormat.format(resources.getString("manage.keystores.open.error"), selected.getUrl()));
        }
    }

    private void addLocalKeystore() {
        final ExtensionFilter filter = new ExtensionFilter(
                resources.getString("manage.keystores.local.filter"),
                "*.jks", "*.JKS", "*.p12", "*.P12", "*.pfx", "*.PFX");
        final File selected = getDisplay().displayFileChooser(filter);
        if (selected == null) {
            return;
        }

        final Path file = selected.toPath().toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) {
            status.setText(MessageFormat.format(resources.getString("manage.keystores.local.invalid"), file));
            return;
        }

        final KeystoreType type = inferKeystoreType(file);
        if (type == null) {
            status.setText(MessageFormat.format(resources.getString("manage.keystores.local.invalid"), file));
            return;
        }

        final String url = file.toUri().toString();
        final boolean alreadyRegistered = observableKeystores.stream()
                .anyMatch(existing -> existing.getUrl().equalsIgnoreCase(url));
        if (alreadyRegistered) {
            status.setText(MessageFormat.format(resources.getString("manage.keystores.local.duplicate"), file));
            return;
        }

        final ConfiguredKeystore configuredKeystore = new ConfiguredKeystore();
        configuredKeystore.setType(type);
        configuredKeystore.setUrl(url);
        configuredKeystore.setToBeSaved(false);
        database.add(configuredKeystore);
        observableKeystores.add(configuredKeystore);
        keystoresTable.getSelectionModel().select(configuredKeystore);
        status.setText(MessageFormat.format(resources.getString("manage.keystores.local.added"), file));
    }

    private static KeystoreType inferKeystoreType(Path file) {
        final String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".jks")) {
            return KeystoreType.JKS;
        }
        if (name.endsWith(".p12") || name.endsWith(".pfx")) {
            return KeystoreType.PKCS12;
        }
        return null;
    }

    private void discoverSmartCards() {
        addSmartCard.setDisable(true);
        status.setText(resources.getString("manage.keystores.smartcard.scanning"));
        CompletableFuture.supplyAsync(this::scanSmartCards)
                .whenComplete((message, error) -> Platform.runLater(() -> {
                    addSmartCard.setDisable(false);
                    if (error != null) {
                        LOGGER.error("Smart-card discovery failed", error);
                        status.setText(MessageFormat.format(
                                resources.getString("manage.keystores.smartcard.error"),
                                rootMessage(error)));
                    } else {
                        status.setText(message);
                    }
                }));
    }

    private String scanSmartCards() {
        try {
            final List<CardTerminal> terminals = TerminalFactory.getDefault().terminals()
                    .list(CardTerminals.State.ALL);
            if (terminals.isEmpty()) {
                return resources.getString("manage.keystores.smartcard.no.reader");
            }

            final List<String> detected = new ArrayList<>();
            for (CardTerminal terminal : terminals) {
                if (!terminal.isCardPresent()) {
                    continue;
                }
                String atr = resources.getString("manage.keystores.smartcard.atr.unavailable");
                Card card = null;
                try {
                    card = terminal.connect("*");
                    atr = DetectedCard.atrToString(card.getATR().getBytes());
                } catch (CardException e) {
                    LOGGER.warn("Card detected in terminal {}, but ATR access failed", terminal.getName(), e);
                } finally {
                    if (card != null) {
                        try {
                            card.disconnect(false);
                        } catch (CardException e) {
                            LOGGER.debug("Cannot disconnect discovery connection from {}", terminal.getName(), e);
                        }
                    }
                }
                detected.add(terminal.getName() + " — ATR: " + atr);
            }

            if (detected.isEmpty()) {
                return MessageFormat.format(resources.getString("manage.keystores.smartcard.no.card"), terminals.size());
            }
            return MessageFormat.format(
                    resources.getString("manage.keystores.smartcard.detected"),
                    detected.size(),
                    String.join(System.lineSeparator(), detected));
        } catch (CardException | RuntimeException e) {
            throw new IllegalStateException(rootMessage(e), e);
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : current.getClass().getSimpleName();
    }
}
