package com.bwdesigngroup.ignition.project_scan.designer;

import com.inductiveautomation.ignition.common.rpc.PushNotificationDeserializer;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonParser;

import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Deserializes JSON bytes received from push notifications into JsonObject.
 * Required for SDK 8.3+ where developers must handle serialization.
 */
public class JsonObjectDeserializer implements PushNotificationDeserializer<JsonObject> {

    @Override
    public JsonObject deserialize(InputStream in) throws IOException {
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        return JsonParser.parseReader(reader).getAsJsonObject();
    }
}
