package com.macuyiko.minecraftpyserver;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.python.util.InteractiveInterpreter;

public class SpigotParser {
	static public boolean parse(final InteractiveInterpreter interpreter, final String buffer, final Object plugin) {
		final boolean[] morea = new boolean[]{false};
		BukkitRunnable r = new BukkitRunnable() {
			public void run() {
				morea[0] = interpreter.runsource(buffer);
			}
		};
		r.runTask((Plugin) plugin);
		return morea[0];
	}
}
