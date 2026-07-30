package lu.nowina.nexu;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the operator-managed TLS material used by the local Spring Boot
 * HTTPS endpoint.
 */
public final class NexuHttpsConfiguration {

    public static final String CERTIFICATE_FILE_NAME = "localhost.cer";
    public static final String PRIVATE_KEY_FILE_NAME = "localhost.key";
    public static final String OPTIONAL_PKCS12_FILE_NAME = "localhost.p12";
    public static final String GUIDE_FILE_NAME = "HTTPS.txt";

    private NexuHttpsConfiguration() {
        // Utility class.
    }

    public static TlsMaterial resolve() throws IOException {
        final Path logFile = NexuLogging.currentLogFile();
        if (logFile == null || logFile.getParent() == null) {
            throw new IOException("NexU logging must be configured before resolving the HTTPS directory");
        }

        final Path logDirectory = logFile.getParent().toAbsolutePath().normalize();
        final Path dataRoot = logDirectory.getParent() != null ? logDirectory.getParent() : logDirectory;
        final Path configDirectory = dataRoot.resolve("config").toAbsolutePath().normalize();
        Files.createDirectories(configDirectory);
        installGuide(configDirectory.resolve(GUIDE_FILE_NAME));

        return new TlsMaterial(
                configDirectory,
                configDirectory.resolve(CERTIFICATE_FILE_NAME),
                configDirectory.resolve(PRIVATE_KEY_FILE_NAME),
                configDirectory.resolve(OPTIONAL_PKCS12_FILE_NAME));
    }

    private static void installGuide(final Path guideFile) throws IOException {
        if (Files.exists(guideFile)) {
            return;
        }
        try (InputStream guide = NexuHttpsConfiguration.class.getResourceAsStream("/https/HTTPS.txt")) {
            if (guide == null) {
                throw new IOException("Packaged HTTPS guide is missing: /https/HTTPS.txt");
            }
            Files.copy(guide, guideFile);
        }
    }

    public record TlsMaterial(
            Path configDirectory,
            Path certificate,
            Path privateKey,
            Path optionalPkcs12) {

        public boolean isComplete() {
            return Files.isRegularFile(certificate) && Files.isRegularFile(privateKey);
        }

        public List<Path> missingFiles() {
            final List<Path> missing = new ArrayList<>(2);
            if (!Files.isRegularFile(certificate)) {
                missing.add(certificate);
            }
            if (!Files.isRegularFile(privateKey)) {
                missing.add(privateKey);
            }
            return List.copyOf(missing);
        }
    }
}
