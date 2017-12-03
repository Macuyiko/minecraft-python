package com.macuyiko.minecraftpyserver.javabridge;

import org.bukkit.scheduler.BukkitRunnable;

public class SpigotRunnable extends BukkitRunnable {
	
	private PythonCallback callback;

	public SpigotRunnable(PythonCallback callback) {
		this.callback = callback;
	}
	        
	@Override
	public void run() {
		callback.callback();
	}

}
