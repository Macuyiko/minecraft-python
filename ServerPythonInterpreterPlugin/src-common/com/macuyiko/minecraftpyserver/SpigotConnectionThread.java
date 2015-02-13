package com.macuyiko.minecraftpyserver;

import java.net.Socket;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpigotConnectionThread extends ConnectionThread {
	
	private boolean more;
	
	public SpigotConnectionThread(Socket socket, SocketServer socketServer) {
		super(socket, socketServer);
	}

	@Override
	protected boolean parse(final String buffer) {
		BukkitRunnable r = new BukkitRunnable() {
			public void run() {
				more = interpreter.runsource(buffer);
			}
		};
		r.runTask((Plugin) server.getPlugin());
		return more;
	}

}
