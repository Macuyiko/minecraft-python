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
}
