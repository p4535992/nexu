/**
 * © Nowina Solutions, 2015-2015
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javafx.application.Application;
import lu.nowina.nexu.api.AppConfig;

public class NexuLauncher {

	public static final String CONFIG_FILE_PROPERTY = "nexu.config.file";
	public static final String CONFIG_FILE_ENVIRONMENT = "NEXU_CONFIG_FILE";
	public static final String CONFIG_FILE_NAME = "nexu-config.properties";

	private static final Logger logger = LoggerFactory.getLogger(NexuLauncher.class.getName());

	private static AppConfig config;

	private static Properties props;

	private static ProxyConfigurer proxyConfigurer;

	public static void main(String[] args) throws Exception {
		NexuLauncher launcher = new NexuLauncher();
		launcher.launch(args);
	}

	public void launch(String[] args) throws IOException {
		props = loadProperties();
		loadAppConfig(props);

		configureLogger(config);

		// Perform this work in a separate method to have the logger well configured.
		config.initDefaultProduct(props);

		proxyConfigurer = new ProxyConfigurer(config, new UserPreferences(config.getApplicationName()));

		beforeLaunch();

		boolean started = checkAlreadyStarted();
		if (!started) {
			NexUApp.launch(getApplicationClass(), args);
		}
	}

	private void configureLogger(AppConfig config) {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		ConsoleAppender console = new ConsoleAppender();
		String pattern = "%d [%p|%c|%C{1}|%t] %m%n";
		console.setLayout(new PatternLayout(pattern));
		console.setThreshold(config.isDebug() ? Level.DEBUG : Level.INFO);
		console.activateOptions();
		org.apache.log4j.Logger.getRootLogger().addAppender(console);

		RollingFileAppender rfa = new RollingFileAppender();
		rfa.setName("FileLogger");
		File nexuHome = config.getNexuHome();
		if (nexuHome == null || (!nexuHome.exists() && !nexuHome.mkdirs())) {
			nexuHome = new File(System.getProperty("java.io.tmpdir"));
		}
		rfa.setFile(new File(nexuHome, "nexu.log").getAbsolutePath());
		rfa.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
		rfa.setThreshold(config.isDebug() ? Level.DEBUG : Level.INFO);
		rfa.setAppend(true);
		rfa.setMaxFileSize(config.getRollingLogMaxFileSize());
		rfa.setMaxBackupIndex(config.getRollingLogMaxFileNumber());
		rfa.activateOptions();
		org.apache.log4j.Logger.getRootLogger().addAppender(rfa);

		org.apache.log4j.Logger.getLogger("org").setLevel(Level.INFO);
		org.apache.log4j.Logger.getLogger("httpclient").setLevel(Level.INFO);
		org.apache.log4j.Logger.getLogger("freemarker").setLevel(Level.INFO);
		org.apache.log4j.Logger.getLogger("lu.nowina").setLevel(config.isDebug() ? Level.DEBUG : Level.INFO);
		org.apache.log4j.Logger.getLogger("java.util.prefs").setLevel(Level.ERROR);
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

	private static boolean checkAlreadyStarted() throws MalformedURLException {
		for (int port : config.getBindingPorts()) {
			final URL url = new URL("http://" + config.getBindingIP() + ":" + port + "/nexu-info");
			final URLConnection connection;
			try {
				connection = url.openConnection();
				connection.setConnectTimeout(2000);
				connection.setReadTimeout(2000);
			} catch (IOException e) {
				logger.warn("IOException when trying to open a connection to " + url + ": " + e.getMessage(), e);
				continue;
			}
			try (InputStream in = connection.getInputStream()) {
				final String info = IOUtils.toString(in);
				logger.error("NexU already started. Version '" + info + "'");
				return true;
			} catch (Exception e) {
				logger.info("No " + url.toString() + " detected, " + e.getMessage());
			}
		}
		return false;
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
}
