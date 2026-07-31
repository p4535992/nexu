/**
 * © Nowina Solutions, 2015-2015
 *
 * Licensed under the EUPL.
 */
package lu.nowina.nexu.view.ui;

import java.net.URL;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import lu.nowina.nexu.flow.StageHelper;
import lu.nowina.nexu.view.ui.support.AbstractUIOperationController;

public class PasswordInputController extends AbstractUIOperationController<char[]> implements Initializable {

    @FXML
    private Button ok;

    @FXML
    private Button cancel;

    @FXML
    private Label passwordPrompt;

    @FXML
    private PasswordField password;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        final EventHandler<ActionEvent> handler = event -> signalEnd(password.getText().toCharArray());
        ok.setOnAction(handler);
        password.setOnAction(handler);
        cancel.setOnAction(event -> signalUserCancel());
    }

    @Override
    public void init(final Object... params) {
        final String applicationName = (String) params[1];
        final String contextualTitle = params.length > 2 ? (String) params[2] : null;
        if (StringUtils.isNotEmpty(contextualTitle)) {
            StageHelper.getInstance().setLiteralTitle(applicationName, contextualTitle);
        } else {
            StageHelper.getInstance().setTitle(applicationName, "password.title");
        }

        final String contextualPrompt = (String) params[0];
        if (StringUtils.isNotEmpty(contextualPrompt)) {
            passwordPrompt.setText(contextualPrompt);
        }
    }
}
