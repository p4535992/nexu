/**
 * © Nowina Solutions, 2015-2017
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package lu.nowina.nexu.systray;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dorkbox.systemTray.Menu;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;
import lu.nowina.nexu.api.SystrayMenuItem;
import lu.nowina.nexu.api.flow.OperationFactory;

/**
 * System tray implementation backed by Dorkbox. On Windows its auto-detection
 * selects the native WindowsNotifyIcon peer and provides better DPI handling
 * than the original JDK AWT tray implementation.
 */
public class DorkboxSystrayMenuInitializer implements SystrayMenuInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DorkboxSystrayMenuInitializer.class.getName());

    @Override
    public boolean init(
            final String tooltip,
            final URL trayIconURL,
            final OperationFactory operationFactory,
            final SystrayMenuItem exitMenuItem,
            final SystrayMenuItem... systrayMenuItems) {

        if (trayIconURL == null) {
            LOGGER.error("Cannot initialize the Dorkbox system tray: /tray-icon.png is missing");
            return false;
        }

        SystemTray.DEBUG = Boolean.parseBoolean(System.getProperty("nexu.systray.debug", "false"));

        SystemTray systemTray = null;
        try {
            LOGGER.info("Attempting Dorkbox system-tray initialization: debug={}, tooltip='{}', icon={}",
                    SystemTray.DEBUG, tooltip, trayIconURL);

            systemTray = SystemTray.get();
            if (systemTray == null) {
                LOGGER.error("Dorkbox returned no compatible system-tray backend");
                return false;
            }

            systemTray.setImage(trayIconURL);
            final Menu menu = systemTray.getMenu();
            for (final SystrayMenuItem systrayMenuItem : systrayMenuItems) {
                menu.add(new MenuItem(systrayMenuItem.getLabel(), event -> invokeMenuAction(
                        systrayMenuItem, operationFactory)));
            }

            final SystemTray initializedTray = systemTray;
            menu.add(new MenuItem(exitMenuItem.getLabel(), event -> {
                try {
                    initializedTray.shutdown();
                } finally {
                    invokeMenuAction(exitMenuItem, operationFactory);
                }
            }));

            LOGGER.info("NexU Dorkbox system tray initialized: implementation={}, menuItems={}, image={}",
                    systemTray.getClass().getName(),
                    systrayMenuItems.length + 1,
                    trayIconURL);
            return true;
        } catch (RuntimeException | LinkageError e) {
            LOGGER.error("Cannot initialize the NexU Dorkbox system tray", e);
            if (systemTray != null) {
                try {
                    systemTray.shutdown();
                } catch (RuntimeException shutdownError) {
                    LOGGER.warn("Unable to shut down the failed Dorkbox tray backend", shutdownError);
                }
            }
            return false;
        }
    }

    private void invokeMenuAction(SystrayMenuItem item, OperationFactory operationFactory) {
        try {
            item.getFutureOperationInvocation().call(operationFactory);
        } catch (RuntimeException e) {
            LOGGER.error("System-tray action '{}' failed", item.getLabel(), e);
        }
    }
}
