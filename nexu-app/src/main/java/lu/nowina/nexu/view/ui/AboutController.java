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
import javafx.scene.control.Label;
import lu.nowina.nexu.view.ui.support.AbstractUIOperationController;

public class AboutController extends AbstractUIOperationController<Void> implements Initializable {

    static final String PROJECT_URL = "https://github.com/p4535992/nexu";
    static final String LICENSE_URL = "https://interoperable-europe.ec.europa.eu/sites/default/files/custom-page/attachment/2020-03/EUPL-1.2%20EN.txt";

    private static final Logger LOGGER = LoggerFactory.getLogger(AboutController.class);

    @FXML
    private Label aboutTitle;

    @FXML
    private Button ok;

    @FXML
    private Label applicationVersion;

    @FXML
    private Label dbVersion;

    @FXML
    private Label dbFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ok.setOnAction(e -> signalEnd(null));
    }

    @Override
    public void init(Object... params) {
        final String applicationName = (String) params[0];
        this.aboutTitle.setText(aboutTitle.getText() + " " + applicationName);

        final String applicationVersion = (String) params[1];
        final String javaVersion = System.getProperty("java.version");
        this.applicationVersion.setText(applicationVersion + ", jvm: " + javaVersion);
    }

    @FXML
    private void openProject() {
        browse(PROJECT_URL);
    }

    @FXML
    private void openLicense() {
        browse(LICENSE_URL);
    }

    private void browse(final String url) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new IOException("Desktop browser integration is not supported");
            }
            Desktop.getDesktop().browse(URI.create(url));
            LOGGER.info("Opened About link {}", url);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Cannot open About link {}", url, e);
        }
    }
}
