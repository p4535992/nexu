package lu.nowina.nexu;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import lu.nowina.nexu.api.AppConfig;

/**
 * Configures the Logback implementation supplied by Spring Boot's starter
 * logging dependency. File rollover is delegated entirely to Logback.
 */
final class NexuLogging {

    static final String LOG_DIRECTORY_ENVIRONMENT = "NEXU_LOG_DIR";
    static final String LOG_DIRECTORY_PROPERTY = "nexu.log.dir";
    static final String LOG_FILE_NAME = "nexu.log";

    private static final String LOG_LEVEL = "log_level";
    private static final String LOG_DIRECTORY = "log_directory";
    private static final String ROLLING_LOG_FILE_SIZE = "rolling_log_file_size";
    private static final String ROLLING_LOG_FILE_NUMBER = "rolling_log_file_number";
    private static final String ROLLING_LOG_TOTAL_SIZE_CAP = "rolling_log_total_size_cap";

    private NexuLogging() {
        // Utility class
    }

    static Path configure(AppConfig config, Properties properties) throws IOException {
        final Path logDirectory = resolveLogDirectory(config, properties);
        Files.createDirectories(logDirectory);

        System.setProperty("NEXU_LOG_DIR", logDirectory.toAbsolutePath().normalize().toString());
        System.setProperty("NEXU_LOG_LEVEL",
                normalizeLevel(properties.getProperty(LOG_LEVEL, "DEBUG")));
        System.setProperty("NEXU_LOG_MAX_FILE_SIZE",
                properties.getProperty(ROLLING_LOG_FILE_SIZE, "10MB").trim());
        System.setProperty("NEXU_LOG_MAX_HISTORY",
                properties.getProperty(ROLLING_LOG_FILE_NUMBER, "14").trim());
        System.setProperty("NEXU_LOG_TOTAL_SIZE_CAP",
                properties.getProperty(ROLLING_LOG_TOTAL_SIZE_CAP, "200MB").trim());

        configureLogback();

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        final Logger logger = LoggerFactory.getLogger(NexuLauncher.class);
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                logger.error("Uncaught exception on thread '{}'", thread.getName(), throwable));

        final Path logFile = logDirectory.resolve(LOG_FILE_NAME);
        logger.info("NexU diagnostic logging initialized: file={}, level={}, maxFileSize={}, maxHistory={}, totalSizeCap={}",
                logFile.toAbsolutePath().normalize(),
                System.getProperty("NEXU_LOG_LEVEL"),
                System.getProperty("NEXU_LOG_MAX_FILE_SIZE"),
                System.getProperty("NEXU_LOG_MAX_HISTORY"),
                System.getProperty("NEXU_LOG_TOTAL_SIZE_CAP"));
        return logFile;
    }

    private static void configureLogback() throws IOException {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final URL configuration = NexuLogging.class.getClassLoader().getResource("logback.xml");
        if (configuration == null) {
            throw new IOException("Logback configuration not found: logback.xml");
        }

        context.reset();
        final JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try {
            configurator.doConfigure(configuration);
        } catch (JoranException e) {
            throw new IOException("Unable to configure Spring Boot Logback logging", e);
        }
    }

    private static Path resolveLogDirectory(AppConfig config, Properties properties) {
        String configured = System.getProperty(LOG_DIRECTORY_PROPERTY);
        if (isBlank(configured)) {
            configured = System.getenv(LOG_DIRECTORY_ENVIRONMENT);
        }
        if (isBlank(configured)) {
            configured = properties.getProperty(LOG_DIRECTORY);
        }
        if (!isBlank(configured)) {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        }

        final File nexuHome = config.getNexuHome();
        if (nexuHome != null) {
            return nexuHome.toPath().resolve("logs").toAbsolutePath().normalize();
        }

        final String userHome = System.getProperty("user.home", System.getProperty("java.io.tmpdir"));
        return Path.of(userHome, ".nexu", "logs").toAbsolutePath().normalize();
    }

    private static String normalizeLevel(String value) {
        if (isBlank(value)) {
            return "DEBUG";
        }
        final String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TRACE", "DEBUG", "INFO", "WARN", "ERROR" -> normalized;
            default -> "DEBUG";
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
