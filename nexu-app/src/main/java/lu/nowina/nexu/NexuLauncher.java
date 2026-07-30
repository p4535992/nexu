/**
 * © Nowina Solutions, 2015-2015
 *
 * Concedée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
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

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import lu.nowina.nexu.WindowsPortOwnerResolver.PortOwner;
import lu.nowina.nexu.api.AppConfig;

public class NexuLauncher {

    public static final String CONFIG_FILE_PROPERTY = "nexu.config.file";
    public static final String CONFIG_FILE_ENVIRONMENT = "NEXU_CONFIG_FILE";
    public static final String CONFIG_FILE_NAME = "nexu-config.properties";

    static final String SYSTRAY_BACKEND_PROPERTY = "nexu.systray.backend";
    static final String SYSTRAY_DEBUG_PROPERTY = "nexu.systray.debug";
    static final String SHOW_ALREADY_RUNNING_DIALOG_PROPERTY = "nexu.show.already.running.dialog";

    private static final Logger logger = LoggerFactory.getLogger(NexuLauncher.class.getName());

    private static AppConfig config;

    private static Properties props;

    private static ProxyConfigurer proxyConfigurer;

    private static UserPreferences userPreferences;

    public static void main(String[] args) throws Exception {
        NexuLauncher launcher = new NexuLauncher();
        launcher.launch(args);
    }

    public void launch(String[] args) throws IOException {
        props = loadProperties();
        loadAppConfig(props);

        userPreferences = new UserPreferences(config.getApplicationName());
        Locale.setDefault(userPreferences.getLanguage().getLocale());
        configureDesktopRuntimeProperties();

        configureLogger(config);

        // Perform this work in a separate method to have the logger well configured.
        config.initDefaultProduct(props);

        proxyConfigurer = new ProxyConfigurer(config, userPreferences);

        beforeLaunch();

        final ExistingInstance existingInstance = findExistingInstance();
        if (existingInstance != null) {
            reportExistingInstance(existingInstance);
            return;
        }

        logger.info("No existing NexU instance detected; launching JavaFX and system-tray lifecycle");
        NexUApp.launch(getApplicationClass(), args);
    }

    private void configureDesktopRuntimeProperties() {
        setRuntimePropertyIfAbsent(SYSTRAY_BACKEND_PROPERTY,
                props.getProperty("systray_backend", "auto"));
        setRuntimePropertyIfAbsent(SYSTRAY_DEBUG_PROPERTY,
                props.getProperty("systray_debug", "false"));
        setRuntimePropertyIfAbsent(SHOW_ALREADY_RUNNING_DIALOG_PROPERTY,
                props.getProperty("show_already_running_dialog", "true"));
    }

    private static void setRuntimePropertyIfAbsent(String propertyName, String value) {
        if (System.getProperty(propertyName) == null) {
            System.setProperty(propertyName, value != null ? value.trim() : "");
        }
    }

    private void configureLogger(AppConfig config) throws IOException {
        final Path logFile = NexuLogging.configure(config, props);
        logger.info("Starting NexU version={} java={} vendor={} os={} arch={} pid={} launcher={} logFile={} systrayBackend={} systrayDebug={}",
                config.getApplicationVersion(),
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                ProcessHandle.current().pid(),
                ProcessHandle.current().info().command().orElse("unknown"),
                logFile.toAbsolutePath().normalize(),
                System.getProperty(SYSTRAY_BACKEND_PROPERTY),
                System.getProperty(SYSTRAY_DEBUG_PROPERTY));
    }

    protected void beforeLaunch() {
        // Do nothing by contract
    }

    public static AppConfig getConfig() {
        return config;
    }

    public static Properties getProperties() {
        return props;
    }

    public static ProxyConfigurer getProxyConfigurer() {
        return proxyConfigurer;
    }

    public static UserPreferences getUserPreferences() {
        return userPreferences;
    }

    private static ExistingInstance findExistingInstance() throws MalformedURLException {
        for (int port : config.getBindingPorts()) {
            final URL url = new URL("http://" + config.getBindingIP() + ":" + port + "/nexu-info");
            final URLConnection connection;
            try {
                connection = url.openConnection();
                connection.setConnectTimeout(2000);
                connection.setReadTimeout(2000);
            } catch (IOException e) {
                logger.warn("Unable to prepare duplicate-instance probe for {}: {}", url, e.getMessage(), e);
                continue;
            }
            try (InputStream in = connection.getInputStream()) {
                final String info = IOUtils.toString(in, StandardCharsets.UTF_8).trim();
                return new ExistingInstance(url, port, info, WindowsPortOwnerResolver.resolve(port));
            } catch (Exception e) {
                logger.info("No NexU endpoint detected at {}: {}", url, e.getMessage());
            }
        }
        return null;
    }

    private static void reportExistingInstance(ExistingInstance existingInstance) {
        final long currentPid = ProcessHandle.current().pid();
        final String ownerPid = existingInstance.owner().map(owner -> Long.toString(owner.pid())).orElse("unknown");
        final String ownerCommand = existingInstance.owner().map(PortOwner::command).orElse("unknown");

        logger.error("Existing NexU instance detected: endpoint={}, response={}, ownerPid={}, ownerCommand={}, "
                        + "currentPid={}. This launcher exits before JavaFX and system-tray initialization. "
                        + "Stop the existing process or use its notification-area icon, then start NexU again.",
                existingInstance.url(),
                existingInstance.info(),
                ownerPid,
                ownerCommand,
                currentPid);

        showAlreadyRunningDialog(existingInstance, ownerPid);
    }

    private static void showAlreadyRunningDialog(ExistingInstance existingInstance, String ownerPid) {
        if (!Boolean.parseBoolean(System.getProperty(SHOW_ALREADY_RUNNING_DIALOG_PROPERTY, "true"))) {
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            logger.warn("Cannot show the duplicate-instance dialog because the desktop session is headless");
            return;
        }

        try {
            final ResourceBundle resources = ResourceBundle.getBundle("bundles/nexu");
            final String message = MessageFormat.format(
                    resources.getString("already.running.message"),
                    existingInstance.url(),
                    ownerPid);
            final Runnable showDialog = () -> JOptionPane.showMessageDialog(
                    null,
                    message,
                    resources.getString("already.running.title"),
                    JOptionPane.WARNING_MESSAGE);

            if (EventQueue.isDispatchThread()) {
                showDialog.run();
            } else {
                EventQueue.invokeAndWait(showDialog);
            }
        } catch (Exception e) {
            logger.warn("Unable to show the duplicate-instance dialog", e);
        }
    }

    private Properties loadProperties() throws IOException {
        final Properties loadedProperties = new Properties();
        loadPropertiesFromClasspath(loadedProperties);

        final File explicitConfig = explicitConfigurationFile();
        if (explicitConfig != null) {
            loadExternalProperties(loadedProperties, explicitConfig, true);
            return loadedProperties;
        }

        for (File candidate : externalConfigurationCandidates()) {
            if (loadExternalProperties(loadedProperties, candidate, false)) {
                break;
            }
        }
        return loadedProperties;
    }

    private static File explicitConfigurationFile() {
        String configuredPath = System.getProperty(CONFIG_FILE_PROPERTY);
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            configuredPath = System.getenv(CONFIG_FILE_ENVIRONMENT);
        }
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            return null;
        }
        return new File(configuredPath.trim()).getAbsoluteFile();
    }

    private static Set<File> externalConfigurationCandidates() {
        final Set<File> candidates = new LinkedHashSet<>();

        final String jpackageApplicationPath = System.getProperty("jpackage.app-path");
        if (jpackageApplicationPath != null && !jpackageApplicationPath.trim().isEmpty()) {
            final File launcher = new File(jpackageApplicationPath).getAbsoluteFile();
            addCandidate(candidates, launcher.getParentFile());
            if (launcher.getParentFile() != null) {
                addCandidate(candidates, launcher.getParentFile().getParentFile());
            }
        }

        final String userDirectory = System.getProperty("user.dir");
        if (userDirectory != null && !userDirectory.trim().isEmpty()) {
            addCandidate(candidates, new File(userDirectory));
        }

        try {
            final URL location = NexuLauncher.class.getProtectionDomain().getCodeSource().getLocation();
            if (location != null && "file".equalsIgnoreCase(location.getProtocol())) {
                final URI locationUri = location.toURI();
                final File codeSource = new File(locationUri).getAbsoluteFile();
                addCandidate(candidates, codeSource.isDirectory() ? codeSource : codeSource.getParentFile());
            }
        } catch (Exception e) {
            logger.debug("Unable to determine code-source directory for external configuration", e);
        }

        return candidates;
    }

    private static void addCandidate(Set<File> candidates, File directory) {
        if (directory != null) {
            candidates.add(new File(directory, CONFIG_FILE_NAME).getAbsoluteFile());
        }
    }

    private static boolean loadExternalProperties(
            Properties target,
            File propertyFile,
            boolean required) throws IOException {

        if (!propertyFile.isFile()) {
            if (required) {
                throw new IOException("Configured NexU properties file does not exist: " + propertyFile);
            }
            return false;
        }

        try (InputStream input = new FileInputStream(propertyFile)) {
            target.load(input);
        }
        logger.info("Loaded external NexU properties from " + propertyFile.getAbsolutePath());
        return true;
    }

    private void loadPropertiesFromClasspath(Properties target) throws IOException {
        try (InputStream configFile = NexUApp.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
            if (configFile == null) {
                throw new IOException("Classpath configuration not found: " + CONFIG_FILE_NAME);
            }
            target.load(configFile);
        }
    }

    public final void loadAppConfig(Properties properties) {
        config = createAppConfig();
        config.loadFromProperties(properties);
    }

    protected AppConfig createAppConfig() {
        return new AppConfig();
    }

    protected Class<? extends Application> getApplicationClass() {
        return NexUApp.class;
    }

    private record ExistingInstance(URL url, int port, String info, Optional<PortOwner> owner) {
    }
}
