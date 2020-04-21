package com.macuyiko.minecraftpyserver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.macuyiko.minecraftpyserver.jython.JyChatServer;
import com.macuyiko.minecraftpyserver.jython.JyCommandExecutor;
import com.macuyiko.minecraftpyserver.jython.JyInterpreter;
import com.macuyiko.minecraftpyserver.jython.JyWebSocketServer;
import com.macuyiko.minecraftpyserver.jython.TelnetServer;

public class MinecraftPyServerPlugin extends JavaPlugin {

	public final static String PLUGIN_NAME = "MinecraftPyServer";
	
	private List<JyInterpreter> pluginInterpreters = new ArrayList<JyInterpreter>();
	
	@Override
	public void onEnable() {
		log("Loading MinecraftPyServerPlugin");
		
		MinecraftPyServerUtils.setup(this.getClassLoader());
		
		int tcpsocketserverport = getConfig().getInt("pythonconsole.telnetport", 44444);
		int websocketserverport = getConfig().getInt("pythonconsole.websocketport", 44445);
		boolean enablechatcommands = getConfig().getString("pythonconsole.enablechatcommands", "true").equalsIgnoreCase("true");
		
		if (tcpsocketserverport > 0)
			startTelnetServer(this, tcpsocketserverport);
		
		if (websocketserverport > 0)
			startWebSocketServer(this, websocketserverport);
		
		if (enablechatcommands) {
			JyChatServer commandServer = startChatServer(this);
			this.getCommand("py").setExecutor(new JyCommandExecutor(this, commandServer));
			this.getCommand("pyrestart").setExecutor(new JyCommandExecutor(this, commandServer));
			this.getCommand("pyload").setExecutor(new JyCommandExecutor(this, commandServer));
		}
		
		File pluginDirectory = new File("./python-plugins/");
		if (pluginDirectory.exists() && pluginDirectory.isDirectory()) {
			File[] files = pluginDirectory.listFiles();
			for (int i = 0; i < files.length; i++) {
			    if (files[i].getName().endsWith(".py")) {
			    	System.err.println("[MinecraftPyServer] Parsing plugin: " + files[i].getName());
			    	JyInterpreter pluginInterpreter = new JyInterpreter(true);
			    	pluginInterpreter.execfile(files[i]);
			    	pluginInterpreters.add(pluginInterpreter);
				}
			}
		}
	}
	
	public void onDisable() {
		log("Unloading MinecraftPyServerPlugin");
		
		for (JyInterpreter pluginInterpreter : pluginInterpreters) {
			pluginInterpreter.close();
		}
	}
	
	public void log(String message) {
		getLogger().info(message);
	}

	public void send(Player player, String message) {
		player.sendMessage(ChatColor.GREEN + message.replace("\r", ""));
	}
	
	public void send(String player, String message) {
		Player p = getServer().getPlayer(player);
		send(p, message);
	}

	public static TelnetServer startTelnetServer(MinecraftPyServerPlugin mainPlugin, int telnetport) {
		TelnetServer server = new TelnetServer(mainPlugin, telnetport);
		Thread t = new Thread(server);
		t.start();
		return server;
	}
	
	public static JyWebSocketServer startWebSocketServer(MinecraftPyServerPlugin mainPlugin, int websocketport) {
		JyWebSocketServer server = new JyWebSocketServer(mainPlugin, websocketport);
		server.start();
		return server;
	}
	
	public static JyChatServer startChatServer(MinecraftPyServerPlugin mainPlugin) {
		JyChatServer server = new JyChatServer(mainPlugin);
		return server;
	}

	
	
	
}
