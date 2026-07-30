/**
 * © Nowina Solutions, 2015-2017
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package lu.nowina.nexu.systray;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lu.nowina.nexu.api.SystrayMenuItem;
import lu.nowina.nexu.api.flow.OperationFactory;

/**
 * Implementation of {@link SystrayMenuInitializer} using AWT.
 */
public class AWTSystrayMenuInitializer implements SystrayMenuInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AWTSystrayMenuInitializer.class.getName());

    private volatile TrayIcon trayIcon;

    @Override
    public boolean init(
            final String tooltip,
            final URL trayIconURL,
            final OperationFactory operationFactory,
            final SystrayMenuItem exitMenuItem,
            final SystrayMenuItem... systrayMenuItems) {

        final AtomicBoolean initialized = new AtomicBoolean(false);
        final Runnable initialization = () -> initialized.set(initializeOnAwtThread(
                tooltip, trayIconURL, operationFactory, exitMenuItem, systrayMenuItems));

        if (EventQueue.isDispatchThread()) {
            initialization.run();
            return initialized.get();
        }

        try {
            EventQueue.invokeAndWait(initialization);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted while initializing the NexU system tray", e);
            return false;
        } catch (final InvocationTargetException e) {
            LOGGER.error("Cannot initialize the NexU system tray", e.getCause());
            return false;
        }
        return initialized.get();
    }

    private boolean initializeOnAwtThread(
            final String tooltip,
            final URL trayIconURL,
            final OperationFactory operationFactory,
            final SystrayMenuItem exitMenuItem,
            final SystrayMenuItem... systrayMenuItems) {

        LOGGER.info("Attempting AWT system-tray initialization: supported={}, headless={}, icon={}",
                SystemTray.isSupported(),
                java.awt.GraphicsEnvironment.isHeadless(),
                trayIconURL);

        if (!SystemTray.isSupported()) {
            LOGGER.error("System tray is not supported by the current desktop session");
            return false;
        }
        if (trayIconURL == null) {
            LOGGER.error("Cannot initialize the NexU system tray: /tray-icon.png is missing");
            return false;
        }

        try {
            final Image image = ImageIO.read(trayIconURL);
            if (image == null) {
                LOGGER.error("Cannot decode the NexU system tray icon from {}", trayIconURL);
                return false;
            }

            final PopupMenu popup = new PopupMenu();
            for (final SystrayMenuItem systrayMenuItem : systrayMenuItems) {
                popup.add(createMenuItem(systrayMenuItem, operationFactory));
            }

            final TrayIcon createdTrayIcon = new TrayIcon(image, tooltip, popup);
            createdTrayIcon.setImageAutoSize(true);

            final MenuItem exitItem = new MenuItem(exitMenuItem.getLabel());
            exitItem.addActionListener(event -> exit(operationFactory, exitMenuItem, createdTrayIcon));
            popup.addSeparator();
            popup.add(exitItem);

            final SystemTray systemTray = SystemTray.getSystemTray();
            if (trayIcon != null) {
                systemTray.remove(trayIcon);
            }
            systemTray.add(createdTrayIcon);
            trayIcon = createdTrayIcon;

            LOGGER.info("NexU AWT system tray initialized: tooltip='{}', menuItems={}, trayIcons={}, image={}x{}",
                    tooltip,
                    systrayMenuItems.length + 1,
                    systemTray.getTrayIcons().length,
                    image.getWidth(null),
                    image.getHeight(null));
            return true;
        } catch (final AWTException | IOException | RuntimeException e) {
            LOGGER.error("Cannot add the NexU icon to the AWT system tray", e);
            return false;
        }
    }

    private MenuItem createMenuItem(
            final SystrayMenuItem systrayMenuItem,
            final OperationFactory operationFactory) {

        final MenuItem menuItem = new MenuItem(systrayMenuItem.getLabel());
        menuItem.addActionListener(event -> {
            try {
                systrayMenuItem.getFutureOperationInvocation().call(operationFactory);
            } catch (final RuntimeException e) {
                LOGGER.error("System-tray action '{}' failed", systrayMenuItem.getLabel(), e);
            }
        });
        return menuItem;
    }

    private void exit(
            final OperationFactory operationFactory,
            final SystrayMenuItem exitMenuItem,
            final TrayIcon iconToRemove) {
        SystemTray.getSystemTray().remove(iconToRemove);
        trayIcon = null;
        exitMenuItem.getFutureOperationInvocation().call(operationFactory);
    }
}
