package com.macuyiko.minecraftpyserver.py4j;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import com.macuyiko.minecraftpyserver.MinecraftPyServerPlugin;

public class SpigotEventListener implements Listener {
	private PyCallback callback;
	private Class<? extends Event> eventtype;

	protected SpigotEventListener(Class<? extends Event> eventtype, PyCallback callback) {
		this.callback = callback;
		this.eventtype = eventtype;
		Bukkit.getServer().getPluginManager().registerEvent(
				eventtype, 
				this, 
				EventPriority.NORMAL, 
				new EventExecutor() {
					@Override
					public void execute(Listener arg0, Event arg1) throws EventException {
						((SpigotEventListener) arg0).execute(arg1);
					}
				}, 
				Bukkit.getServer().getPluginManager().getPlugin(MinecraftPyServerPlugin.PLUGIN_NAME));
	}

	public void execute(Event eventtype) {
		callback.callback(eventtype);
	}
	
	public void remove() {
		HandlerList.unregisterAll(this);
	}

	public Class<? extends Event> getEventType() {
		return eventtype;
	}
}
