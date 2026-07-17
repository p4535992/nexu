# JavaFX boundary inventory

## Core Java files importing or referencing JavaFX
nexu-core/src/main/java/lu/nowina/nexu/view/core/UIOperation.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/APISelectionController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/ConfigureKeystoreController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/KeySelectionController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/ManageKeystoresController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/MessageController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/PasswordInputController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/Pkcs11ParamsController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/ProductSelectionController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/SaveKeystoreController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/UnsupportedProductController.java

## Core UI/view Java files
nexu-core/src/main/java/lu/nowina/nexu/view/core/AbstractUIOperationController.java
nexu-core/src/main/java/lu/nowina/nexu/view/core/ExtensionFilter.java
nexu-core/src/main/java/lu/nowina/nexu/view/core/NonBlockingUIOperation.java
nexu-core/src/main/java/lu/nowina/nexu/view/core/UIDisplay.java
nexu-core/src/main/java/lu/nowina/nexu/view/core/UIOperation.java
nexu-core/src/main/java/lu/nowina/nexu/view/core/UIOperationController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/APISelectionController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/ConfigureKeystoreController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/KeySelectionController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/ManageKeystoresController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/MessageController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/PasswordInputController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/Pkcs11ParamsController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/ProductSelectionController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/SaveKeystoreController.java
nexu-core/src/main/java/lu/nowina/nexu/view/ui/UnsupportedProductController.java

## Core UI resources
nexu-core/src/main/resources/fxml/api-selection.fxml
nexu-core/src/main/resources/fxml/configure-keystore.fxml
nexu-core/src/main/resources/fxml/key-selection.fxml
nexu-core/src/main/resources/fxml/manage-keystores.fxml
nexu-core/src/main/resources/fxml/message-no-button.fxml
nexu-core/src/main/resources/fxml/message.fxml
nexu-core/src/main/resources/fxml/password-input.fxml
nexu-core/src/main/resources/fxml/pkcs11-params.fxml
nexu-core/src/main/resources/fxml/product-selection.fxml
nexu-core/src/main/resources/fxml/save-keystore.fxml
nexu-core/src/main/resources/fxml/unsupported-product.fxml
nexu-core/src/main/resources/images/medal.png
nexu-core/src/main/resources/images/quality.png
nexu-core/src/main/resources/images/unlocked.png

## References to core UI packages from core
nexu-core/src/main/java/lu/nowina/nexu/APIBuilder.java:34:import lu.nowina.nexu.view.core.UIDisplay;
nexu-core/src/main/java/lu/nowina/nexu/InternalAPI.java:59:import lu.nowina.nexu.view.core.UIDisplay;
nexu-core/src/main/java/lu/nowina/nexu/flow/AbstractCoreFlow.java:20:import lu.nowina.nexu.view.core.UIDisplay;
nexu-core/src/main/java/lu/nowina/nexu/flow/BasicFlowRegistry.java:20:import lu.nowina.nexu.view.core.UIDisplay;
nexu-core/src/main/java/lu/nowina/nexu/flow/Flow.java:21:import lu.nowina.nexu.view.core.UIDisplay;
nexu-core/src/main/java/lu/nowina/nexu/flow/Flow.java:22:import lu.nowina.nexu.view.core.UIOperation;
nexu-core/src/main/java/lu/nowina/nexu/flow/FlowRegistry.java:17:import lu.nowina.nexu.view.core.UIDisplay;
nexu-core/src/main/java/lu/nowina/nexu/flow/GetCertificateFlow.java:49:import lu.nowina.nexu.view.core.UIDisplay;
nexu-core/src/main/java/lu/nowina/nexu/flow/GetCertificateFlow.java:50:import lu.nowina.nexu.view.core.UIOperation;
nexu-core/src/main/java/lu/nowina/nexu/flow/LogoutFlow.java:36:import lu.nowina.nexu.view.core.UIDisplay;
nexu-core/src/main/java/lu/nowina/nexu/flow/LogoutFlow.java:37:import lu.nowina.nexu.view.core.UIOperation;
nexu-core/src/main/java/lu/nowina/nexu/flow/SignatureFlow.java:42:import lu.nowina.nexu.view.core.UIDisplay;
nexu-core/src/main/java/lu/nowina/nexu/flow/SignatureFlow.java:43:import lu.nowina.nexu.view.core.UIOperation;
nexu-core/src/main/java/lu/nowina/nexu/flow/operation/AbstractCompositeOperation.java:17:import lu.nowina.nexu.view.core.UIDisplay;
nexu-core/src/main/java/lu/nowina/nexu/flow/operation/AdvancedCreationFeedbackOperation.java:26:import lu.nowina.nexu.view.core.UIOperation;
nexu-core/src/main/java/lu/nowina/nexu/flow/operation/BasicOperationFactory.java:18:import lu.nowina.nexu.view.core.UIDisplay;
nexu-core/src/main/java/lu/nowina/nexu/flow/operation/CompositeOperation.java:18:import lu.nowina.nexu.view.core.UIDisplay;
nexu-core/src/main/java/lu/nowina/nexu/flow/operation/CompositeOperation.java:19:import lu.nowina.nexu.view.core.UIOperation;
nexu-core/src/main/java/lu/nowina/nexu/flow/operation/CreateTokenOperation.java:40:import lu.nowina.nexu.view.core.UIOperation;
nexu-core/src/main/java/lu/nowina/nexu/flow/operation/SelectPrivateKeyOperation.java:31:import lu.nowina.nexu.view.core.UIOperation;
nexu-core/src/main/java/lu/nowina/nexu/flow/operation/UIDisplayAwareOperation.java:17:import lu.nowina.nexu.view.core.UIDisplay;
nexu-core/src/main/java/lu/nowina/nexu/flow/operation/UIDisplayAwareOperation.java:18:import lu.nowina.nexu.view.core.UIOperation;
nexu-core/src/main/java/lu/nowina/nexu/keystore/KeystoreProductAdapter.java:49:import lu.nowina.nexu.view.core.NonBlockingUIOperation;
nexu-core/src/main/java/lu/nowina/nexu/keystore/KeystoreProductAdapter.java:50:import lu.nowina.nexu.view.core.UIOperation;
nexu-core/src/main/java/lu/nowina/nexu/view/core/AbstractUIOperationController.java:14:package lu.nowina.nexu.view.core;
nexu-core/src/main/java/lu/nowina/nexu/view/core/ExtensionFilter.java:14:package lu.nowina.nexu.view.core;
nexu-core/src/main/java/lu/nowina/nexu/view/core/NonBlockingUIOperation.java:14:package lu.nowina.nexu.view.core;
nexu-core/src/main/java/lu/nowina/nexu/view/core/UIDisplay.java:14:package lu.nowina.nexu.view.core;
nexu-core/src/main/java/lu/nowina/nexu/view/core/UIOperation.java:14:package lu.nowina.nexu.view.core;
nexu-core/src/main/java/lu/nowina/nexu/view/core/UIOperationController.java:14:package lu.nowina.nexu.view.core;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/APISelectionController.java:14:package lu.nowina.nexu.view.ui;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/APISelectionController.java:31:import lu.nowina.nexu.view.core.AbstractUIOperationController;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/ConfigureKeystoreController.java:14:package lu.nowina.nexu.view.ui;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/ConfigureKeystoreController.java:31:import lu.nowina.nexu.view.core.AbstractUIOperationController;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/ConfigureKeystoreController.java:32:import lu.nowina.nexu.view.core.ExtensionFilter;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/KeySelectionController.java:14:package lu.nowina.nexu.view.ui;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/KeySelectionController.java:56:import lu.nowina.nexu.view.core.AbstractUIOperationController;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/ManageKeystoresController.java:14:package lu.nowina.nexu.view.ui;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/ManageKeystoresController.java:35:import lu.nowina.nexu.view.core.AbstractUIOperationController;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/MessageController.java:14:package lu.nowina.nexu.view.ui;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/MessageController.java:26:import lu.nowina.nexu.view.core.AbstractUIOperationController;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/PasswordInputController.java:14:package lu.nowina.nexu.view.ui;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/PasswordInputController.java:29:import lu.nowina.nexu.view.core.AbstractUIOperationController;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/Pkcs11ParamsController.java:14:package lu.nowina.nexu.view.ui;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/Pkcs11ParamsController.java:30:import lu.nowina.nexu.view.core.AbstractUIOperationController;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/Pkcs11ParamsController.java:31:import lu.nowina.nexu.view.core.ExtensionFilter;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/ProductSelectionController.java:14:package lu.nowina.nexu.view.ui;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/ProductSelectionController.java:34:import lu.nowina.nexu.view.core.AbstractUIOperationController;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/SaveKeystoreController.java:14:package lu.nowina.nexu.view.ui;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/SaveKeystoreController.java:27:import lu.nowina.nexu.view.core.AbstractUIOperationController;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/UnsupportedProductController.java:14:package lu.nowina.nexu.view.ui;
nexu-core/src/main/java/lu/nowina/nexu/view/ui/UnsupportedProductController.java:28:import lu.nowina.nexu.view.core.AbstractUIOperationController;

## References to core UI packages from app
nexu-app/src/main/java/lu/nowina/nexu/NexUApp.java:41:import lu.nowina.nexu.view.core.UIDisplay;
nexu-app/src/main/java/lu/nowina/nexu/StandaloneUIDisplay.java:36:import lu.nowina.nexu.view.core.ExtensionFilter;
nexu-app/src/main/java/lu/nowina/nexu/StandaloneUIDisplay.java:37:import lu.nowina.nexu.view.core.NonBlockingUIOperation;
nexu-app/src/main/java/lu/nowina/nexu/StandaloneUIDisplay.java:38:import lu.nowina.nexu.view.core.UIDisplay;
nexu-app/src/main/java/lu/nowina/nexu/StandaloneUIDisplay.java:39:import lu.nowina.nexu.view.core.UIOperation;
nexu-app/src/main/java/lu/nowina/nexu/SystrayMenu.java:30:import lu.nowina.nexu.view.core.NonBlockingUIOperation;
nexu-app/src/main/java/lu/nowina/nexu/view/ui/AboutController.java:14:package lu.nowina.nexu.view.ui;
nexu-app/src/main/java/lu/nowina/nexu/view/ui/AboutController.java:23:import lu.nowina.nexu.view.core.AbstractUIOperationController;
nexu-app/src/main/java/lu/nowina/nexu/view/ui/AbstractFeedbackUIOperationController.java:14:package lu.nowina.nexu.view.ui;
nexu-app/src/main/java/lu/nowina/nexu/view/ui/AbstractFeedbackUIOperationController.java:23:import lu.nowina.nexu.view.core.AbstractUIOperationController;
nexu-app/src/main/java/lu/nowina/nexu/view/ui/AbstractFeedbackUIOperationController.java:24:import lu.nowina.nexu.view.core.UIOperationController;
nexu-app/src/main/java/lu/nowina/nexu/view/ui/PreferencesController.java:14:package lu.nowina.nexu.view.ui;
nexu-app/src/main/java/lu/nowina/nexu/view/ui/PreferencesController.java:34:import lu.nowina.nexu.view.core.AbstractUIOperationController;
nexu-app/src/main/java/lu/nowina/nexu/view/ui/ProvideFeedbackController.java:14:package lu.nowina.nexu.view.ui;
nexu-app/src/main/java/lu/nowina/nexu/view/ui/StoreResultController.java:14:package lu.nowina.nexu.view.ui;

## JavaFX dependencies in core POM
127-        <dependency>
128-            <groupId>org.openjfx</groupId>
129:            <artifactId>javafx-controls</artifactId>
130-            <version>${javafx.version}</version>
131-        </dependency>
132-        <dependency>
133-            <groupId>org.openjfx</groupId>
134:            <artifactId>javafx-fxml</artifactId>
135-            <version>${javafx.version}</version>
136-        </dependency>
137-    </dependencies>
138-
