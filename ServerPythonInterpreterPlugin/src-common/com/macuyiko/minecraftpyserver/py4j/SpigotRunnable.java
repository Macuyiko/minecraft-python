package com.macuyiko.minecraftpyserver.py4j;

import org.bukkit.scheduler.BukkitRunnable;

public class SpigotRunnable extends BukkitRunnable {
	
	private PyCallback callback;

	public SpigotRunnable(PyCallback callback) {
		this.callback = callback;
	}
	        
	@Override
	public void run() {
		boolean result = callback.callback();
		if (!result)
			System.err.print("[MinecraftPyServer] Result of a PyCallback was False");
	}

}
