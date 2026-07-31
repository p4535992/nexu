package lu.nowina.nexu;

import java.util.ResourceBundle;

/**
 * Resolves clear, localized password text for local file-keystore operations.
 *
 * <p>The DSS browser workflow has two stages: certificate selection and
 * private-key signing. When the token remains open, one password can cover both
 * stages. When it is closed after certificate selection, a later signing call
 * has to reopen it and may request the password again.</p>
 */
final class PasswordPromptMessages {

    private static final String BUNDLE_NAME = "bundles/password-prompts";

    private PasswordPromptMessages() {
    }

    static Prompt resolve(
            final boolean closeToken,
            final int passwordRequestIndex,
            final String adapterPrompt) {

        final ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME);
        if (adapterPrompt != null && !adapterPrompt.isBlank()) {
            return new Prompt(bundle.getString("password.title.generic"), adapterPrompt);
        }

        if (!closeToken) {
            return localized(bundle, "combined");
        }
        return localized(bundle, passwordRequestIndex == 0 ? "certificate" : "private.key");
    }

    private static Prompt localized(final ResourceBundle bundle, final String suffix) {
        return new Prompt(
                bundle.getString("password.title." + suffix),
                bundle.getString("password.prompt." + suffix));
    }

    record Prompt(String title, String message) {
    }
}
