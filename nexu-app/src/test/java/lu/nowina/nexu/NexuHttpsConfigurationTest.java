package lu.nowina.nexu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NexuHttpsConfigurationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void generatesUniqueCompletePemMaterialBesideLogs() throws Exception {
        withLogDirectory(temporaryDirectory.resolve("generated").resolve("logs"), () -> {
            final NexuHttpsConfiguration.TlsMaterial generated = NexuHttpsConfiguration.resolve();
            assertEquals("localhost.crt", generated.certificate().getFileName().toString());
            assertEquals("localhost.key", generated.privateKey().getFileName().toString());
            assertTrue(generated.isComplete());
            assertTrue(Files.isRegularFile(generated.configDirectory().resolve("HTTPS.txt")));
            assertTrue(Files.readString(generated.privateKey()).contains("BEGIN PRIVATE KEY"));

            final X509Certificate certificate;
            try (InputStream input = Files.newInputStream(generated.certificate())) {
                certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(input);
            }
            certificate.checkValidity();
            assertTrue(certificate.getSubjectX500Principal().getName().contains("CN=localhost"));
            final Collection<List<?>> subjectAlternativeNames = certificate.getSubjectAlternativeNames();
            assertTrue(subjectAlternativeNames.stream().anyMatch(name -> "localhost".equals(name.get(1))));
            assertTrue(subjectAlternativeNames.stream().anyMatch(name -> "127.0.0.1".equals(name.get(1))));

            final String certificateBefore = Files.readString(generated.certificate());
            final String keyBefore = Files.readString(generated.privateKey());
            final NexuHttpsConfiguration.TlsMaterial resolvedAgain = NexuHttpsConfiguration.resolve();
            assertEquals(certificateBefore, Files.readString(resolvedAgain.certificate()));
            assertEquals(keyBefore, Files.readString(resolvedAgain.privateKey()));
        });
    }

    @Test
    void partialOperatorConfigurationIsReportedAndNeverOverwritten() throws Exception {
        final Path dataRoot = temporaryDirectory.resolve("partial");
        final Path config = dataRoot.resolve("config");
        Files.createDirectories(config);
        Files.writeString(config.resolve("localhost.crt"), "operator certificate");

        withLogDirectory(dataRoot.resolve("logs"), () -> {
            final NexuHttpsConfiguration.TlsMaterial material = NexuHttpsConfiguration.resolve();
            assertFalse(material.isComplete());
            assertEquals(List.of(config.resolve("localhost.key").toAbsolutePath().normalize()), material.missingFiles());
            assertEquals("operator certificate", Files.readString(material.certificate()));
        });
    }

    @Test
    void legacyCerCertificateNameRemainsSupported() throws Exception {
        final Path dataRoot = temporaryDirectory.resolve("legacy");
        final Path config = dataRoot.resolve("config");
        Files.createDirectories(config);
        Files.writeString(config.resolve("localhost.cer"), "legacy certificate");
        Files.writeString(config.resolve("localhost.key"), "legacy key");

        withLogDirectory(dataRoot.resolve("logs"), () -> {
            final NexuHttpsConfiguration.TlsMaterial material = NexuHttpsConfiguration.resolve();
            assertTrue(material.isComplete());
            assertEquals("localhost.cer", material.certificate().getFileName().toString());
        });
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

    private void withLogDirectory(Path logDirectory, ThrowingRunnable action) throws Exception {
        final String previousLogDirectory = System.getProperty(NexuLogging.LOG_DIRECTORY_ENVIRONMENT);
        try {
            System.setProperty(NexuLogging.LOG_DIRECTORY_ENVIRONMENT, logDirectory.toString());
            action.run();
        } finally {
            if (previousLogDirectory == null) {
                System.clearProperty(NexuLogging.LOG_DIRECTORY_ENVIRONMENT);
            } else {
                System.setProperty(NexuLogging.LOG_DIRECTORY_ENVIRONMENT, previousLogDirectory);
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
