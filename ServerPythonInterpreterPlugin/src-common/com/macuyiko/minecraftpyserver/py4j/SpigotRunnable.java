package com.macuyiko.minecraftpyserver.py4j;

import org.bukkit.scheduler.BukkitRunnable;

public class SpigotRunnable extends BukkitRunnable {
	
	private PyCallback callback;

	public SpigotRunnable(PyCallback callback) {
		this.callback = callback;
	}
	        
	@Override
	public void run() {
		try {
			callback.callback();
		} catch (Exception e) {
			System.err.println("---------");
			e.printStackTrace();
		}
	}

}
