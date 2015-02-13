package com.macuyiko.bukkitconsole;

import java.io.IOException;

import org.bukkit.plugin.java.JavaPlugin;

import com.macuyiko.minecraftpyserver.ConsolePlugin;
import com.macuyiko.minecraftpyserver.SetupUtils;

public class MainPlugin extends JavaPlugin {

	public MainPlugin() {

	}

	@Override
	public void onEnable() {
		getLogger().info("Loading Python Console");
		try {
			int serverport = getConfig().getInt("pythonconsole.serverconsole.port", 44444);
			String serverpass = getConfig().getString("pythonconsole.serverconsole.password", "swordfish");
			int serverconns = getConfig().getInt("pythonconsole.serverconsole.maxconnections", 10);
			SetupUtils.setup();
			ConsolePlugin.start(this, serverport, serverpass, serverconns);
		} catch (IOException e) {
			getLogger().severe(e.getMessage());
			return;
		}
	}
}
