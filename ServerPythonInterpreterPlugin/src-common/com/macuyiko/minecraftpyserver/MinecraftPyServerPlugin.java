package com.macuyiko.minecraftpyserver;

import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.macuyiko.minecraftpyserver.jython.JyChatServer;
import com.macuyiko.minecraftpyserver.jython.JyCommandExecutor;
import com.macuyiko.minecraftpyserver.jython.JyHTTPServer;
import com.macuyiko.minecraftpyserver.jython.JyInterpreter;
import com.macuyiko.minecraftpyserver.jython.JyWebSocketServer;
import com.macuyiko.minecraftpyserver.jython.TelnetServer;

public class MinecraftPyServerPlugin extends JavaPlugin {

	public final static String PLUGIN_NAME = "MinecraftPyServer";
	
	@SuppressWarnings("unused")
	private static JyInterpreter pluginInterpreter = null;
	
	@Override
	public void onEnable() {
		log("Loading MinecraftPyServerPlugin");
		try {
			MinecraftPyServerUtils.setup();
			
			int tcpsocketserverport = getConfig().getInt("pythonconsole.serverconsole.telnetport", 44444);
			int websocketserverport = getConfig().getInt("pythonconsole.serverconsole.websocketport", 44445);
			int httpport = getConfig().getInt("pythonconsole.serverconsole.httpeditorport", 8080);
			boolean enablechatcommands = getConfig().getString("pythonconsole.serverconsole.enablechatcommands", "true").equalsIgnoreCase("true");
			if (tcpsocketserverport > 0)
				startTelnetServer(this, tcpsocketserverport);
			if (websocketserverport > 0)
				startWebSocketServer(this, websocketserverport);
			if (httpport > 0)
				JyHTTPServer.start(httpport, "lib-http");
			if (enablechatcommands) {
				JyChatServer commandServer = startChatServer(this);
				this.getCommand("py").setExecutor(new JyCommandExecutor(this, commandServer));
				this.getCommand("pyrestart").setExecutor(new JyCommandExecutor(this, commandServer));
				this.getCommand("pyload").setExecutor(new JyCommandExecutor(this, commandServer));
			}
			
			pluginInterpreter = new JyInterpreter(false, true);
		} catch (IOException e) {
			getLogger().severe(e.getMessage());
			return;
		}
	}
	
	public void log(String message) {
		getLogger().info(message);
	}

	public void send(String player, String message) {
		Player p = getServer().getPlayer(player);
		p.sendMessage(ChatColor.GREEN + message.replace("\r", ""));
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
