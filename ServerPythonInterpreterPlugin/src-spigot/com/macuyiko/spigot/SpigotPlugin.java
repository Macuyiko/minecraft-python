package com.macuyiko.spigot;

import java.io.File;
import java.io.IOException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.python.core.PyException;

import com.macuyiko.minecraftpyserver.ConsolePlugin;
import com.macuyiko.minecraftpyserver.PyInterpreter;
import com.macuyiko.minecraftpyserver.PyInterpreterRunnable;
import com.macuyiko.minecraftpyserver.PyPlugin;
import com.macuyiko.minecraftpyserver.SetupUtils;

public class SpigotPlugin extends JavaPlugin implements PyPlugin {
	
	@Override
	public void onEnable() {
		log("Loading Python Console");
		try {
			int tcpsocketserverport = getConfig().getInt("pythonconsole.serverconsole.telnetport", 44444);
    		int websocketserverport = getConfig().getInt("pythonconsole.serverconsole.websocketport", 44445);
    		String serverpass = getConfig().getString("pythonconsole.serverconsole.password", "swordfish");
    		SetupUtils.setup();
			ConsolePlugin.start(this, tcpsocketserverport, websocketserverport, serverpass);
		} catch (IOException e) {
			getLogger().severe(e.getMessage());
			return;
		}
	}
		
	@Override
	public void log(String message) {
		getLogger().info(message);
	}
	
	public boolean parse(final PyInterpreter interpreter, final String code, final boolean exec) throws PyException {
		final PyInterpreterRunnable runnable = new PyInterpreterRunnable(this, interpreter, code, exec);
		runnable.run();
		return runnable.more();
	}

	public boolean parse(final PyInterpreter interpreter, final File script) throws PyException {
		final PyInterpreterRunnable runnable = new PyInterpreterRunnable(this, interpreter, script);
		runnable.run();
		return runnable.more();
	}
	
	public boolean parseR(final PyInterpreter interpreter, final String code, final boolean exec) throws PyException {
		final PyInterpreterRunnable runnable = new PyInterpreterRunnable(this, interpreter, code, exec);
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				runnable.run();
			}
		};
		r.runTask(this);
		return runnable.more();
	}

	public boolean parseR(final PyInterpreter interpreter, final File script) throws PyException {
		final PyInterpreterRunnable runnable = new PyInterpreterRunnable(this, interpreter, script);
		BukkitRunnable r = new BukkitRunnable() {
			@Override
			public void run() {
				runnable.run();
			}
		};
		r.runTask(this);
		return runnable.more();
	}
}
