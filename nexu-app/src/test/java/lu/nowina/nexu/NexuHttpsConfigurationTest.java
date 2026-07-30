package lu.nowina.nexu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NexuHttpsConfigurationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void configDirectoryIsCreatedBesideLogsAndRequiresBothPemFiles() throws Exception {
        final String previousLogDirectory = System.getProperty(NexuLogging.LOG_DIRECTORY_ENVIRONMENT);
        final Path logDirectory = temporaryDirectory.resolve("logs");
        try {
            System.setProperty(NexuLogging.LOG_DIRECTORY_ENVIRONMENT, logDirectory.toString());

            final NexuHttpsConfiguration.TlsMaterial missing = NexuHttpsConfiguration.resolve();
            assertEquals(temporaryDirectory.resolve("config").toAbsolutePath().normalize(),
                    missing.configDirectory());
            assertTrue(Files.isRegularFile(missing.configDirectory().resolve("HTTPS.txt")));
            assertFalse(missing.isComplete());
            assertEquals(2, missing.missingFiles().size());

            Files.writeString(missing.certificate(), "test certificate");
            assertFalse(NexuHttpsConfiguration.resolve().isComplete(),
                    "A certificate without its matching private key must not enable HTTPS");

            Files.writeString(missing.privateKey(), "test private key");
            assertTrue(NexuHttpsConfiguration.resolve().isComplete());
        } finally {
            if (previousLogDirectory == null) {
                System.clearProperty(NexuLogging.LOG_DIRECTORY_ENVIRONMENT);
            } else {
                System.setProperty(NexuLogging.LOG_DIRECTORY_ENVIRONMENT, previousLogDirectory);
            }
        }
    }

    @Test
    void springBootServerUsesTheConfiguredHttpsPortAndExternalPemFiles() throws Exception {
        final String source = Files.readString(Path.of(
                "src", "main", "java", "lu", "nowina", "nexu", "springboot", "server",
                "SpringBootHttpServer.java"));

        assertTrue(source.contains("getBindingPortsHttps()"));
        assertTrue(source.contains("server.ssl.certificate"));
        assertTrue(source.contains("server.ssl.certificate-private-key"));
        assertTrue(source.contains("https://localhost:{}/nexu-info"));
    }
}
