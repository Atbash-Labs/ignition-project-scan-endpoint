package com.bwdesigngroup.ignition.project_scan.designer;

import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inductiveautomation.ignition.designer.IgnitionDesigner;

public class AutoSaveManager {
    private final Logger logger = LoggerFactory.getLogger("project-scan.AutoSave");
    private final IgnitionDesigner designer;
    private ScheduledExecutorService scheduler;
    private volatile AbstractButton cachedSaveButton;

    public AutoSaveManager(IgnitionDesigner designer) {
        this.designer = designer;
    }

    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            logger.warn("AutoSave is already running");
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "autosave-scheduler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(
            () -> SwingUtilities.invokeLater(this::triggerSave),
            1, 1, TimeUnit.SECONDS
        );

        logger.info("AutoSave started with 1-second interval");
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
            cachedSaveButton = null;
            logger.info("AutoSave stopped");
        }
    }

    private void triggerSave() {
        try {
            if (cachedSaveButton == null || !cachedSaveButton.isShowing()) {
                cachedSaveButton = findSaveButton();
            }

            if (cachedSaveButton != null && cachedSaveButton.isEnabled()) {
                cachedSaveButton.doClick(0);
                logger.trace("AutoSave triggered");
            }
        } catch (Exception e) {
            logger.error("AutoSave error", e);
        }
    }

    /**
     * Searches for the Save button by first scanning toolbar components,
     * then falling back to the File menu's Save item.
     */
    private AbstractButton findSaveButton() {
        AbstractButton btn = findButtonInToolbars(designer.getContentPane());
        if (btn != null) {
            logger.debug("Found save button in toolbar");
            return btn;
        }

        btn = findSaveMenuItem();
        if (btn != null) {
            logger.debug("Found save menu item as fallback");
        } else {
            logger.warn("Could not locate save button or menu item");
        }
        return btn;
    }

    private AbstractButton findButtonInToolbars(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof AbstractButton) {
                AbstractButton btn = (AbstractButton) c;
                if (isSaveButton(btn)) {
                    return btn;
                }
            }
            if (c instanceof Container) {
                AbstractButton found = findButtonInToolbars((Container) c);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private boolean isSaveButton(AbstractButton btn) {
        String tooltip = btn.getToolTipText();
        String text = btn.getText();
        String actionCmd = btn.getActionCommand();

        return matchesSave(tooltip) || matchesSave(text) || matchesSave(actionCmd);
    }

    private boolean matchesSave(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase().trim();
        return lower.equals("save") || lower.equals("save project");
    }

    private JMenuItem findSaveMenuItem() {
        JMenuBar menuBar = designer.getJMenuBar();
        if (menuBar == null) {
            return null;
        }

        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu == null) {
                continue;
            }
            String menuText = menu.getText();
            if (menuText != null && menuText.equalsIgnoreCase("File")) {
                for (int j = 0; j < menu.getItemCount(); j++) {
                    JMenuItem item = menu.getItem(j);
                    if (item != null && matchesSave(item.getText())) {
                        return item;
                    }
                }
            }
        }
        return null;
    }
}
