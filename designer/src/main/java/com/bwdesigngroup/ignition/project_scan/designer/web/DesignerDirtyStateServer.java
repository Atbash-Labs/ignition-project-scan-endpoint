package com.bwdesigngroup.ignition.project_scan.designer.web;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.resourcecollection.Resource;
import com.inductiveautomation.ignition.common.resourcecollection.ResourceId;
import com.inductiveautomation.ignition.designer.model.DesignerContext;
import com.inductiveautomation.ignition.designer.project.DesignableProject;

public class DesignerDirtyStateServer {
	private static final Logger logger = LoggerFactory.getLogger("project-scan.DirtyStateServer");
	private static final String PORT_PROPERTY = "project.scan.designer.port";
	private static final int DEFAULT_PORT = 8199;

	private final DesignerContext context;
	private HttpServer server;

	public DesignerDirtyStateServer(DesignerContext context) {
		this.context = context;
	}

	public void start() {
		int port = Integer.getInteger(PORT_PROPERTY, DEFAULT_PORT);
		try {
			server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
		} catch (BindException e) {
			logger.warn("Port {} already in use, falling back to OS-assigned port", port);
			try {
				server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
			} catch (IOException ex) {
				logger.error("Failed to start dirty state server on fallback port", ex);
				return;
			}
		} catch (IOException e) {
			logger.error("Failed to start dirty state server on port {}", port, e);
			return;
		}

		server.createContext("/dirty", this::handleDirtyCheck);
		server.setExecutor(null);
		server.start();
		logger.info("Dirty state endpoint listening on port {}", server.getAddress().getPort());
	}

	public void stop() {
		if (server != null) {
			server.stop(0);
			logger.info("Dirty state server stopped");
		}
	}

	private void handleDirtyCheck(HttpExchange exchange) throws IOException {
		if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
			exchange.sendResponseHeaders(405, -1);
			return;
		}

		JsonObject response = new JsonObject();
		try {
			DesignableProject project = context.getProject();
			Map<ResourceId, Resource> resources = project.getAllResources();
			boolean isDirty = false;

			for (Resource resource : resources.values()) {
				if (project.isChanged(resource.getResourcePath())) {
					isDirty = true;
					break;
				}
			}

			response.addProperty("dirty", isDirty);
		} catch (Exception e) {
			logger.error("Error checking project dirty state", e);
			response.addProperty("error", e.getMessage());
			byte[] body = response.toString().getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(500, body.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(body);
			}
			return;
		}

		byte[] body = response.toString().getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, body.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(body);
		}
	}
}
