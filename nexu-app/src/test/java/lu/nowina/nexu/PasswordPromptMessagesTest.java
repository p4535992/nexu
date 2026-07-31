package lu.nowina.nexu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PasswordPromptMessagesTest {

    private final Locale originalLocale = Locale.getDefault();

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    @Test
    void closeTokenUsesSeparateCertificateAndPrivateKeyPrompts() {
        Locale.setDefault(Locale.ENGLISH);

        final PasswordPromptMessages.Prompt certificate =
                PasswordPromptMessages.resolve(true, 0, null);
        final PasswordPromptMessages.Prompt privateKey =
                PasswordPromptMessages.resolve(true, 1, null);

        assertEquals("Keystore certificate access", certificate.title());
        assertTrue(certificate.message().contains("read the certificates"));
        assertEquals("Private-key signing", privateKey.title());
        assertTrue(privateKey.message().contains("unlock the selected private key"));
    }

    @Test
    void openTokenUsesOneCombinedPrompt() {
        Locale.setDefault(Locale.ENGLISH);

        final PasswordPromptMessages.Prompt prompt =
                PasswordPromptMessages.resolve(false, 0, null);

        assertEquals("Certificate selection and signing", prompt.title());
        assertTrue(prompt.message().contains("both certificate selection"));
        assertTrue(prompt.message().contains("private-key signing"));
    }

    @Test
    void adapterPinPromptIsNotOverwritten() {
        Locale.setDefault(Locale.ENGLISH);

        final PasswordPromptMessages.Prompt prompt =
                PasswordPromptMessages.resolve(true, 0, "PIN code");

        assertEquals("Password required", prompt.title());
        assertEquals("PIN code", prompt.message());
    }

    @Test
    void italianMessagesExplainBothStages() {
        Locale.setDefault(Locale.ITALIAN);

        final PasswordPromptMessages.Prompt certificate =
                PasswordPromptMessages.resolve(true, 0, null);
        final PasswordPromptMessages.Prompt privateKey =
                PasswordPromptMessages.resolve(true, 1, null);
        final PasswordPromptMessages.Prompt combined =
                PasswordPromptMessages.resolve(false, 0, null);

        assertTrue(certificate.message().contains("certificati disponibili"));
        assertTrue(privateKey.message().contains("chiave privata"));
        assertTrue(combined.message().contains("sia per selezionare il certificato sia per firmare"));
    }
}
