package com.bwdesigngroup.ignition.project_scan.designer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.client.gateway_interface.PushNotificationListener;
import com.inductiveautomation.ignition.designer.IgnitionDesigner;
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
                logger.debug("Performing force update");
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

    private Boolean showDialog() {
        ConfirmationDialog dialog = new ConfirmationDialog(IgnitionDesigner.getFrame());
        dialog.setLocationRelativeTo(IgnitionDesigner.getFrame());
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
        return dialog.isConfirmed();
    }
}