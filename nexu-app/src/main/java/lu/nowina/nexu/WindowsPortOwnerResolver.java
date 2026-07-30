package lu.nowina.nexu;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

/**
 * Windows process diagnostics and replacement support for the NexU listening
 * port. A process is terminated only after the launcher has independently
 * confirmed the NexU HTTP endpoint on that port.
 */
final class WindowsPortOwnerResolver {

    private static final long COMMAND_TIMEOUT_SECONDS = 4;
    private static final Duration PROCESS_EXIT_TIMEOUT = Duration.ofSeconds(10);

    private WindowsPortOwnerResolver() {
        // Utility class
    }

    static Optional<PortOwner> resolve(int port) {
        if (!isWindows()) {
            return Optional.empty();
        }

        OptionalLong pid = resolveWithPowerShell(port);
        if (pid.isEmpty()) {
            pid = resolveWithNetstat(port);
        }
        if (pid.isEmpty()) {
            return Optional.empty();
        }

        final long processId = pid.getAsLong();
        final String command = ProcessHandle.of(processId)
                .flatMap(process -> process.info().command())
                .orElse("unknown");
        return Optional.of(new PortOwner(processId, command));
    }

    static boolean forceTerminate(PortOwner owner) {
        if (!isWindows() || owner == null || owner.pid() <= 0 || owner.pid() == ProcessHandle.current().pid()) {
            return false;
        }

        final Optional<ProcessHandle> processHandle = ProcessHandle.of(owner.pid());
        if (processHandle.isEmpty()) {
            return true;
        }

        final ProcessHandle process = processHandle.get();
        if (!process.isAlive()) {
            return true;
        }

        process.destroyForcibly();
        if (waitUntilStopped(process, PROCESS_EXIT_TIMEOUT)) {
            return true;
        }

        if (!runTaskkill(owner.pid())) {
            return false;
        }
        return waitUntilStopped(process, PROCESS_EXIT_TIMEOUT);
    }

    static OptionalLong parsePowerShellPid(String output) {
        if (output == null) {
            return OptionalLong.empty();
        }
        for (String line : output.lines().toList()) {
            final String candidate = line.trim();
            if (!candidate.isEmpty()) {
                try {
                    return OptionalLong.of(Long.parseLong(candidate));
                } catch (NumberFormatException ignored) {
                    // Continue looking for a numeric line.
                }
            }
        }
        return OptionalLong.empty();
    }

    static OptionalLong parseNetstat(String output, int port) {
        if (output == null) {
            return OptionalLong.empty();
        }
        final String expectedSuffix = ":" + port;
        for (String line : output.lines().toList()) {
            final String[] columns = line.trim().split("\\s+");
            if (columns.length < 5 || !"TCP".equalsIgnoreCase(columns[0])) {
                continue;
            }
            if (!columns[1].endsWith(expectedSuffix) || !"LISTENING".equalsIgnoreCase(columns[3])) {
                continue;
            }
            try {
                return OptionalLong.of(Long.parseLong(columns[4]));
            } catch (NumberFormatException ignored) {
                // Ignore malformed netstat rows.
            }
        }
        return OptionalLong.empty();
    }

    private static OptionalLong resolveWithPowerShell(int port) {
        final String command = "$connection = Get-NetTCPConnection -State Listen -LocalPort " + port
                + " -ErrorAction SilentlyContinue | Select-Object -First 1; "
                + "if ($connection) { $connection.OwningProcess }";
        return executeAndParse(new String[] {
                "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", command
        }, WindowsPortOwnerResolver::parsePowerShellPid);
    }

    private static OptionalLong resolveWithNetstat(int port) {
        return executeAndParse(new String[] { "netstat.exe", "-ano", "-p", "TCP" },
                output -> parseNetstat(output, port));
    }

    private static boolean runTaskkill(long pid) {
        Process process = null;
        try {
            process = new ProcessBuilder(
                    "taskkill.exe", "/PID", Long.toString(pid), "/T", "/F")
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0 || ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false) == false;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static boolean waitUntilStopped(ProcessHandle process, Duration timeout) {
        final Instant deadline = Instant.now().plus(timeout);
        while (process.isAlive() && Instant.now().isBefore(deadline)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return !process.isAlive();
    }

    private static OptionalLong executeAndParse(String[] command, OutputParser parser) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return OptionalLong.empty();
            }
            final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                return OptionalLong.empty();
            }
            return parser.parse(output);
        } catch (IOException e) {
            return OptionalLong.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return OptionalLong.empty();
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
    }

    record PortOwner(long pid, String command) {
    }

    @FunctionalInterface
    private interface OutputParser {
        OptionalLong parse(String output);
    }
}
