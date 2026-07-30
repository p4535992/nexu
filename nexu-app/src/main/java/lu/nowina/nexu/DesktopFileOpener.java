package lu.nowina.nexu;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens local files with the desktop association and conservative platform
 * fallbacks when no association exists.
 */
public final class DesktopFileOpener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DesktopFileOpener.class);

    private DesktopFileOpener() {
        // Utility class.
    }

    public static void openWithDefaultApplication(final Path file) throws IOException {
        open(file, false);
    }

    public static void openWithDefaultTextEditor(final Path file) throws IOException {
        open(file, true);
    }

    private static void open(final Path file, final boolean textFile) throws IOException {
        final Path normalized = requireRegularFile(file);
        final List<IOException> failures = new ArrayList<>();

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(normalized.toFile());
                LOGGER.info("Opened {} with the operating-system default application", normalized);
                return;
            }
            failures.add(new IOException("Desktop open action is not supported"));
        } catch (IOException | RuntimeException e) {
            failures.add(asIOException("Desktop association failed for " + normalized, e));
        }

        for (List<String> command : fallbackCommands(System.getProperty("os.name", ""), normalized, textFile)) {
            try {
                new ProcessBuilder(command).start();
                LOGGER.info("Opened {} using fallback command {}", normalized, command.get(0));
                return;
            } catch (IOException | RuntimeException e) {
                failures.add(asIOException("Fallback command failed: " + command, e));
            }
        }

        final IOException failure = new IOException("No application could open " + normalized);
        failures.forEach(failure::addSuppressed);
        throw failure;
    }

    static List<List<String>> fallbackCommands(final String osName, final Path file, final boolean textFile) {
        final String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        final String path = file.toAbsolutePath().normalize().toString();
        final List<List<String>> commands = new ArrayList<>();

        if (os.contains("win")) {
            if (textFile) {
                commands.add(List.of("notepad.exe", path));
            }
            commands.add(List.of("cmd.exe", "/c", "start", "", path));
        } else if (os.contains("mac")) {
            commands.add(textFile ? List.of("open", "-e", path) : List.of("open", path));
        } else {
            commands.add(List.of("xdg-open", path));
            commands.add(List.of("gio", "open", path));
            if (textFile) {
                commands.add(List.of("gedit", path));
                commands.add(List.of("kate", path));
                commands.add(List.of("xed", path));
                commands.add(List.of("mousepad", path));
            }
        }
        return List.copyOf(commands);
    }

    private static Path requireRegularFile(final Path file) throws IOException {
        if (file == null) {
            throw new IOException("No file was provided");
        }
        final Path normalized = file.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) {
            throw new IOException("File does not exist or is not a regular file: " + normalized);
        }
        return normalized;
    }

    private static IOException asIOException(final String message, final Throwable cause) {
        return cause instanceof IOException io ? io : new IOException(message, cause);
    }
}
