package com.bwdesigngroup.ignition.project_scan.designer;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inductiveautomation.ignition.designer.IgnitionDesigner;

public class AutoSaveManager {
    private final Logger logger = LoggerFactory.getLogger("project-scan.AutoSave");
    private final IgnitionDesigner designer;
    private ScheduledExecutorService scheduler;
    private Robot robot;

    public AutoSaveManager(IgnitionDesigner designer) {
        this.designer = designer;
    }

    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            logger.warn("AutoSave is already running");
            return;
        }

        try {
            robot = new Robot();
        } catch (Exception e) {
            logger.error("Failed to create Robot for autosave — autosave will not function", e);
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "autosave-scheduler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::triggerSave, 1, 1, TimeUnit.SECONDS);
        logger.info("AutoSave started with 1-second interval");
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
            robot = null;
            logger.info("AutoSave stopped");
        }
    }

    private void triggerSave() {
        try {
            if (!designer.isShowing()) {
                return;
            }

            SwingUtilities.invokeAndWait(() -> designer.toFront());

            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_S);
            robot.keyRelease(KeyEvent.VK_S);
            robot.keyRelease(KeyEvent.VK_CONTROL);

            logger.trace("AutoSave triggered via Ctrl+S");
        } catch (Exception e) {
            logger.error("AutoSave error", e);
        }
    }
}
