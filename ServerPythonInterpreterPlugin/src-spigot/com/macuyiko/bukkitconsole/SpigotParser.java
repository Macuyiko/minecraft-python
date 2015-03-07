package com.macuyiko.bukkitconsole;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.python.util.InteractiveInterpreter;

public class SpigotParser {
	static public boolean parse(final InteractiveInterpreter interpreter, final String code, final boolean exec, final Object plugin) throws Exception {
		try{
			final boolean[] morea = new boolean[]{false};
			BukkitRunnable r = new BukkitRunnable() {
				public void run() {
					if (exec) {
						interpreter.exec(code);
					} else {
						morea[0] = interpreter.runsource(code);
					}				
				}
			};
			r.runTask((Plugin) plugin);
			return morea[0];
		} catch (Throwable e) {
			throw new Exception(e);
		}
	}
}
