package com.bwdesigngroup.ignition.project_scan.designer;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inductiveautomation.ignition.common.gateway.messages.PushNotification;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.Gson;
import com.inductiveautomation.ignition.client.gateway_interface.FilteredPushNotificationListener;
import com.inductiveautomation.ignition.designer.IgnitionDesigner;
import com.inductiveautomation.ignition.designer.project.ConflictResolutionDialog;
import com.bwdesigngroup.ignition.project_scan.designer.dialog.ConfirmationDialog;
import com.bwdesigngroup.ignition.project_scan.common.ProjectScanConstants;

public class DesignerPushNotificationListener extends FilteredPushNotificationListener {
    private final Logger logger = LoggerFactory
            .getLogger(ProjectScanConstants.MODULE_ID + ".designerPushNotificationListener");
    private final IgnitionDesigner designer;
    private final Gson gson = new Gson();
    private volatile boolean isDialogShowing = false;
    private volatile boolean pendingForceUpdate = false;

    public DesignerPushNotificationListener(IgnitionDesigner designer) {
        super(ProjectScanConstants.MODULE_ID, new String[] { ProjectScanConstants.DESIGNER_SCAN_NOTIFICATION_ID });
        this.designer = designer;
    }

    @Override
    public synchronized void receive(PushNotification notification) {
        logger.info("Received push notification: " + notification.getMessageType());

        if (!notification.getMessageType().equals(ProjectScanConstants.DESIGNER_SCAN_NOTIFICATION_ID)) {
            return;
        }

        // Parse the notification message
        Object notificationMessage = notification.getMessage();
        JsonObject notificationData = gson.fromJson(notificationMessage.toString(), JsonObject.class);
        boolean forceUpdate = notificationData != null && notificationData.has("forceUpdate")
                && notificationData.get("forceUpdate").getAsBoolean();

        // If force update is requested, set the flag
        if (forceUpdate) {
            pendingForceUpdate = true;
        }

        // If a dialog is already showing, just update the pending force update flag and return
        if (isDialogShowing) {
            logger.debug("Dialog already showing, updating pending force update flag");
            return;
        }

        // Show the dialog or perform force update
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
            // Reset state after handling update
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