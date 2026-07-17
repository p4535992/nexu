package lu.nowina.nexu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import lu.nowina.nexu.api.AppConfig;

class NexuLoggingTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void windowsPortableWritesLogsBesideNexuExecutable() throws Exception {
        final Path applicationRoot = temporaryDirectory.resolve("NexU");
        Files.createDirectories(applicationRoot);
        Files.createFile(applicationRoot.resolve(NexuLogging.PORTABLE_MARKER_FILE));

        final Path resolved = NexuLogging.resolveLogDirectory(
                new AppConfig(),
                new Properties(),
                null,
                null,
                applicationRoot.resolve("NexU.exe").toString());

        assertEquals(applicationRoot.resolve("logs").toAbsolutePath().normalize(), resolved);
    }

    @Test
    void linuxPortableWritesLogsAtApplicationImageRoot() throws Exception {
        final Path applicationRoot = temporaryDirectory.resolve("NexU");
        final Path binDirectory = applicationRoot.resolve("bin");
        Files.createDirectories(binDirectory);
        Files.createFile(applicationRoot.resolve(NexuLogging.PORTABLE_MARKER_FILE));

        final Path resolved = NexuLogging.resolveLogDirectory(
                new AppConfig(),
                new Properties(),
                null,
                null,
                binDirectory.resolve("NexU").toString());

        assertEquals(applicationRoot.resolve("logs").toAbsolutePath().normalize(), resolved);
    }

    @Test
    void explicitLogDirectoryOverridesPortableLocation() throws Exception {
        final Path applicationRoot = temporaryDirectory.resolve("NexU");
        final Path explicitDirectory = temporaryDirectory.resolve("diagnostics");
        Files.createDirectories(applicationRoot);
        Files.createFile(applicationRoot.resolve(NexuLogging.PORTABLE_MARKER_FILE));

        final Path resolved = NexuLogging.resolveLogDirectory(
                new AppConfig(),
                new Properties(),
                explicitDirectory.toString(),
                null,
                applicationRoot.resolve("NexU.exe").toString());

        assertEquals(explicitDirectory.toAbsolutePath().normalize(), resolved);
    }
}
