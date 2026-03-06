package com.bwdesigngroup.ignition.project_scan.designer;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.client.gateway_interface.PushNotificationListener;
import com.inductiveautomation.ignition.designer.IgnitionDesigner;
import com.inductiveautomation.ignition.designer.project.ConflictResolutionDialog;
import com.bwdesigngroup.ignition.project_scan.designer.dialog.ConfirmationDialog;
import com.bwdesigngroup.ignition.project_scan.common.ProjectScanConstants;

/**
 * Push notification listener for designer project scan updates.
 * SDK 8.3+ requires implementing PushNotificationListener<T> directly.
 */
public class DesignerPushNotificationListener implements PushNotificationListener<JsonObject> {
    private final Logger logger = LoggerFactory
            .getLogger(ProjectScanConstants.MODULE_ID + ".designerPushNotificationListener");
    protected final IgnitionDesigner designer;
    private volatile boolean isDialogShowing = false;
    private volatile boolean pendingForceUpdate = false;

    public DesignerPushNotificationListener(IgnitionDesigner designer) {
        this.designer = designer;
    }

    @Override
    public String moduleId() {
        return ProjectScanConstants.MODULE_ID;
    }

    @Override
    public String messageType() {
        return ProjectScanConstants.DESIGNER_SCAN_NOTIFICATION_ID;
    }

    @Override
    public synchronized void receiveNotification(JsonObject notificationData) {
        logger.info("Received push notification for project scan");
        boolean forceUpdate = notificationData != null && notificationData.has("forceUpdate")
                && notificationData.get("forceUpdate").getAsBoolean();

        if (forceUpdate) {
            pendingForceUpdate = true;
        }

        if (isDialogShowing) {
            logger.debug("Dialog already showing, updating pending force update flag");
            return;
        }

        handleUpdate();
    }

    private synchronized void handleUpdate() {
        if (isDialogShowing) {
            return;
        }

        isDialogShowing = true;

        try {
            if (pendingForceUpdate) {
                logger.debug("Performing force update — will auto-accept gateway conflicts");
                startConflictDialogWatcher();
                this.designer.updateProject();
            } else {
                if (showDialog()) {
                    this.designer.updateProject();
                }
            }
        } finally {
            isDialogShowing = false;
            pendingForceUpdate = false;
        }
    }

    /**
     * Spawns a background thread that watches for Ignition's
     * ConflictResolutionDialog. When detected, it clicks
     * "Gateway" (use all from gateway) then "Apply".
     */
    private void startConflictDialogWatcher() {
        Thread watcher = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    Thread.sleep(100);
                    Window[] windows = Window.getWindows();
                    for (Window w : windows) {
                        if (w instanceof ConflictResolutionDialog && w.isShowing()) {
                            logger.info("ConflictResolutionDialog detected, auto-resolving with Gateway");
                            SwingUtilities.invokeLater(() -> autoResolveConflictDialog((ConflictResolutionDialog) w));
                            return;
                        }
                    }
                }
                logger.debug("No ConflictResolutionDialog appeared (no conflicts)");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "conflict-dialog-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    private void autoResolveConflictDialog(ConflictResolutionDialog dialog) {
        JButton gatewayBtn = findButtonByText(dialog, "Gateway");
        if (gatewayBtn != null) {
            logger.debug("Clicking 'Gateway' button");
            gatewayBtn.doClick();
        }

        JButton applyBtn = findButtonByText(dialog, "Apply");
        if (applyBtn != null) {
            logger.debug("Clicking 'Apply' button");
            applyBtn.doClick();
        } else {
            logger.warn("Could not find 'Apply' button in ConflictResolutionDialog");
        }
    }

    private JButton findButtonByText(Container container, String text) {
        for (Component c : container.getComponents()) {
            if (c instanceof JButton && text.equals(((JButton) c).getText())) {
                return (JButton) c;
            }
            if (c instanceof Container) {
                JButton found = findButtonByText((Container) c, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private Boolean showDialog() {
        ConfirmationDialog dialog = new ConfirmationDialog(IgnitionDesigner.getFrame());
        dialog.setLocationRelativeTo(IgnitionDesigner.getFrame());
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
        return dialog.isConfirmed();
    }
}