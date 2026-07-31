package lu.nowina.nexu.flow;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StageHelper {

    private static StageHelper instance;

    private static final Logger LOGGER = LoggerFactory.getLogger(StageHelper.class);

    private String title;

    private ResourceBundle bundle;

    private static final String BUNDLE_NAME = "bundles/nexu";

    private StageHelper() {
    }

    public static synchronized StageHelper getInstance() {
        if (instance == null) {
            synchronized (StageHelper.class) {
                if (instance == null) {
                    instance = new StageHelper();
                    instance.setBundle(ResourceBundle.getBundle(BUNDLE_NAME));
                }
            }
        }
        return instance;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String applicationName, final String resourceBundleKey) {
        if (isBlank(applicationName) && isBlank(resourceBundleKey)) {
            title = "";
            return;
        }
        String translatedTitle = "";
        try {
            translatedTitle = getBundle().getString(resourceBundleKey);
        } catch (MissingResourceException exception) {
            LOGGER.warn("Resource bundle key \"{}\" is missing.", resourceBundleKey);
        } catch (Exception exception) {
            LOGGER.error(exception.getMessage(), exception);
        }
        setLiteralTitle(applicationName, translatedTitle);
    }

    /** Sets a title that has already been localized by a feature-specific bundle. */
    public void setLiteralTitle(final String applicationName, final String translatedTitle) {
        if (!isBlank(applicationName) && !isBlank(translatedTitle)) {
            title = applicationName + " - " + translatedTitle;
        } else if (isBlank(applicationName)) {
            title = isBlank(translatedTitle) ? "" : translatedTitle;
        } else {
            title = applicationName;
        }
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public void setBundle(final ResourceBundle bundle) {
        this.bundle = bundle;
    }
}
