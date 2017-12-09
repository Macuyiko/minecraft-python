package com.macuyiko.minecraftpyserver.py4j;

import org.bukkit.event.Event;

public interface PyCallback {
	void callback();
	void callback(String label, String[] parameters);
	void callback(Event eventtype);
}