package lu.nowina.nexu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class DesktopFileOpenerTest {

    @Test
    void windowsTextFilesFallbackToNotepadBeforeShellAssociation() {
        final List<List<String>> commands = DesktopFileOpener.fallbackCommands(
                "Windows 11", Path.of("C:/NexU/logs/nexu.log"), true);

        assertEquals("notepad.exe", commands.get(0).get(0));
        assertEquals("cmd.exe", commands.get(1).get(0));
        assertTrue(commands.get(1).contains("start"));
    }

    @Test
    void linuxUsesDesktopOpenersAndTextEditors() {
        final List<List<String>> commands = DesktopFileOpener.fallbackCommands(
                "Linux", Path.of("/tmp/nexu.log"), true);

        assertEquals("xdg-open", commands.get(0).get(0));
        assertEquals("gio", commands.get(1).get(0));
        assertTrue(commands.stream().anyMatch(command -> command.get(0).equals("gedit")));
    }
}
