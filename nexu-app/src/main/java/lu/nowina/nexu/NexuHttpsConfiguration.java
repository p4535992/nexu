package lu.nowina.nexu;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves and, when necessary, creates the per-installation TLS material used
 * by the local Spring Boot HTTPS endpoint.
 */
public final class NexuHttpsConfiguration {

    public static final String CERTIFICATE_FILE_NAME = "localhost.crt";
    public static final String LEGACY_CERTIFICATE_FILE_NAME = "localhost.cer";
    public static final String PRIVATE_KEY_FILE_NAME = "localhost.key";
    public static final String OPTIONAL_PKCS12_FILE_NAME = "localhost.p12";
    public static final String GUIDE_FILE_NAME = "HTTPS.txt";

    private static final Logger LOGGER = LoggerFactory.getLogger(NexuHttpsConfiguration.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private NexuHttpsConfiguration() {
        // Utility class.
    }

    public static synchronized TlsMaterial resolve() throws IOException {
        final Path logFile = NexuLogging.currentLogFile();
        if (logFile == null || logFile.getParent() == null) {
            throw new IOException("NexU logging must be configured before resolving the HTTPS directory");
        }

        final Path logDirectory = logFile.getParent().toAbsolutePath().normalize();
        final Path dataRoot = logDirectory.getParent() != null ? logDirectory.getParent() : logDirectory;
        final Path configDirectory = dataRoot.resolve("config").toAbsolutePath().normalize();
        Files.createDirectories(configDirectory);
        installGuide(configDirectory.resolve(GUIDE_FILE_NAME));

        final Path primaryCertificate = configDirectory.resolve(CERTIFICATE_FILE_NAME);
        final Path legacyCertificate = configDirectory.resolve(LEGACY_CERTIFICATE_FILE_NAME);
        final Path privateKey = configDirectory.resolve(PRIVATE_KEY_FILE_NAME);

        if (!Files.exists(primaryCertificate) && !Files.exists(legacyCertificate) && !Files.exists(privateKey)) {
            generateSelfSignedLocalhostCertificate(primaryCertificate, privateKey);
            LOGGER.info("Generated a per-installation self-signed localhost certificate: certificate={}, privateKey={}",
                    primaryCertificate, privateKey);
        }

        final Path selectedCertificate = Files.isRegularFile(primaryCertificate)
                ? primaryCertificate
                : Files.isRegularFile(legacyCertificate) ? legacyCertificate : primaryCertificate;

        if (Files.isRegularFile(legacyCertificate) && !Files.isRegularFile(primaryCertificate)) {
            LOGGER.info("Using legacy localhost certificate file {}; rename it to {} when convenient",
                    legacyCertificate, primaryCertificate);
        }

        return new TlsMaterial(
                configDirectory,
                selectedCertificate,
                privateKey,
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

    private static void generateSelfSignedLocalhostCertificate(final Path certificateFile, final Path privateKeyFile)
            throws IOException {
        final Path certificateTemp = Files.createTempFile(certificateFile.getParent(), "localhost-", ".crt.tmp");
        final Path privateKeyTemp = Files.createTempFile(privateKeyFile.getParent(), "localhost-", ".key.tmp");
        try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048, SECURE_RANDOM);
            final KeyPair keyPair = keyPairGenerator.generateKeyPair();

            final Instant now = Instant.now();
            final X500Name subject = new X500Name("CN=localhost,O=NexU Local HTTPS");
            final BigInteger serial = new BigInteger(160, SECURE_RANDOM).abs();
            final X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    subject,
                    serial,
                    Date.from(now.minus(5, ChronoUnit.MINUTES)),
                    Date.from(now.plus(3650, ChronoUnit.DAYS)),
                    subject,
                    keyPair.getPublic());
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            builder.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
            builder.addExtension(Extension.extendedKeyUsage, false,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
            builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName[] {
                    new GeneralName(GeneralName.dNSName, "localhost"),
                    new GeneralName(GeneralName.iPAddress, "127.0.0.1")
            }));

            final ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
            final X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(builder.build(signer));
            certificate.checkValidity();
            certificate.verify(keyPair.getPublic());

            writePem(certificateTemp, "CERTIFICATE", certificate.getEncoded());
            writePem(privateKeyTemp, "PRIVATE KEY", keyPair.getPrivate().getEncoded());
            restrictPrivateKeyPermissions(privateKeyTemp);

            moveIntoPlace(certificateTemp, certificateFile);
            moveIntoPlace(privateKeyTemp, privateKeyFile);
            restrictPrivateKeyPermissions(privateKeyFile);
        } catch (GeneralSecurityException | OperatorCreationException e) {
            throw new IOException("Cannot generate the NexU localhost certificate", e);
        } finally {
            Files.deleteIfExists(certificateTemp);
            Files.deleteIfExists(privateKeyTemp);
        }
    }

    private static void writePem(final Path file, final String type, final byte[] encoded) throws IOException {
        final String lineSeparator = System.lineSeparator();
        final String body = Base64.getMimeEncoder(64, lineSeparator.getBytes(StandardCharsets.US_ASCII))
                .encodeToString(encoded);
        Files.writeString(file,
                "-----BEGIN " + type + "-----" + lineSeparator
                        + body + lineSeparator
                        + "-----END " + type + "-----" + lineSeparator,
                StandardCharsets.US_ASCII);
    }

    private static void moveIntoPlace(final Path source, final Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }
    }

    private static void restrictPrivateKeyPermissions(final Path privateKey) throws IOException {
        try {
            final Set<PosixFilePermission> permissions = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(privateKey, permissions);
        } catch (UnsupportedOperationException e) {
            // Windows uses ACLs rather than POSIX modes. The file inherits the user's
            // protected data-directory permissions.
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
