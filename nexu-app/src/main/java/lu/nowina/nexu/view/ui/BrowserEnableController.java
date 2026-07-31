package lu.nowina.nexu.view.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.view.ui.support.AbstractUIOperationController;

/** Explains how to trust the local NexU HTTPS certificate in a browser. */
public class BrowserEnableController extends AbstractUIOperationController<Void> implements Initializable {

    static final String DEFAULT_ENDPOINT = "https://localhost:9895/nexu-info";

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserEnableController.class);
    private static final String BUNDLE_NAME = "bundles/browser-enable";

    @FXML
    private Label heading;

    @FXML
    private Label description;

    @FXML
    private Label instructions;

    @FXML
    private Label endpointLabel;

    @FXML
    private Hyperlink endpoint;

    @FXML
    private Label security;

    @FXML
    private Label status;

    @FXML
    private Button openBrowser;

    @FXML
    private Button close;

    private ResourceBundle messages;

    @Override
    public void initialize(final URL location, final ResourceBundle ignored) {
        messages = ResourceBundle.getBundle(BUNDLE_NAME);
        heading.setText(messages.getString("browser.enable.heading"));
        description.setText(messages.getString("browser.enable.description"));
        instructions.setText(messages.getString("browser.enable.instructions"));
        endpointLabel.setText(messages.getString("browser.enable.endpoint.label"));
        security.setText(messages.getString("browser.enable.security"));
        openBrowser.setText(messages.getString("browser.enable.open"));
        close.setText(messages.getString("browser.enable.close"));

        endpoint.setOnAction(event -> openEndpoint());
        openBrowser.setOnAction(event -> openEndpoint());
        close.setOnAction(event -> signalEnd(null));
    }

    @Override
    public void init(final Object... params) {
        final String applicationName = params.length > 0 && params[0] instanceof String value ? value : "NexU";
        final String endpointUrl = params.length > 1 && params[1] instanceof String value && !value.isBlank()
                ? value
                : DEFAULT_ENDPOINT;
        endpoint.setText(endpointUrl);
        StageHelper.getInstance().setLiteralTitle(
                applicationName,
                messages.getString("browser.enable.dialog.title"));
    }

    private void openEndpoint() {
        final String endpointUrl = endpoint.getText();
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new IOException("Desktop browser integration is not supported");
            }
            Desktop.getDesktop().browse(URI.create(endpointUrl));
            status.setText(messages.getString("browser.enable.status.opened"));
            LOGGER.info("Opened NexU browser verification endpoint {}", endpointUrl);
        } catch (IOException | RuntimeException exception) {
            status.setText(messages.getString("browser.enable.status.error"));
            LOGGER.error("Cannot open NexU browser verification endpoint " + endpointUrl, exception);
        }
    }
}
