package com.macuyiko.pylink;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;

public class ScriptRegistrationTracker {

	private final Deque<AutoCloseable> resources = new ArrayDeque<AutoCloseable>();

	public synchronized void track(AutoCloseable resource) {
		if (resource != null) {
			resources.push(resource);
		}
	}

	public synchronized void cleanupAll(Logger logger, String scriptName) {
		while (!resources.isEmpty()) {
			AutoCloseable resource = resources.pop();
			try {
				resource.close();
			} catch (Exception e) {
				if (logger != null) {
					logger.warning("Failed to clean up resource for script '" + scriptName + "': " + e.getMessage());
				}
			}
		}
	}
}
