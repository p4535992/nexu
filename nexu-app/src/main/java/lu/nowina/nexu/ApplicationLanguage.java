package lu.nowina.nexu;

import java.util.Locale;

/**
 * Languages supported by the NexU desktop user interface.
 */
enum ApplicationLanguage {

    ENGLISH("en", Locale.ENGLISH, "language.english"),
    ITALIAN("it", Locale.ITALIAN, "language.italian");

    private final String code;
    private final Locale locale;
    private final String labelKey;

    ApplicationLanguage(String code, Locale locale, String labelKey) {
        this.code = code;
        this.locale = locale;
        this.labelKey = labelKey;
    }

    String getCode() {
        return code;
    }

    Locale getLocale() {
        return locale;
    }

    String getLabelKey() {
        return labelKey;
    }

    static ApplicationLanguage fromCode(String code) {
        if (code != null) {
            for (ApplicationLanguage language : values()) {
                if (language.code.equalsIgnoreCase(code.trim())) {
                    return language;
                }
            }
        }
        return ENGLISH;
    }
}
