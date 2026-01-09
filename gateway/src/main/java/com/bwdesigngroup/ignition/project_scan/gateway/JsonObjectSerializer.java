package com.bwdesigngroup.ignition.project_scan.gateway;

import com.inductiveautomation.ignition.common.rpc.PushNotificationSerializer;
import com.inductiveautomation.ignition.common.gson.JsonObject;

import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Serializes JsonObject into bytes for sending via push notifications.
 * Required for SDK 8.3+ where developers must handle serialization.
 */
public class JsonObjectSerializer implements PushNotificationSerializer<JsonObject> {

    @Override
    public void serialize(JsonObject message, OutputStream out) throws IOException {
        out.write(message.toString().getBytes(StandardCharsets.UTF_8));
    }
}
