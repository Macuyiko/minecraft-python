package com.macuyiko.minecraftpyserver;

import java.io.IOException;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.macuyiko.minecraftpyserver.jython.JyChatServer;
import com.macuyiko.minecraftpyserver.jython.JyCommandExecutor;
import com.macuyiko.minecraftpyserver.jython.JyHTTPServer;
import com.macuyiko.minecraftpyserver.jython.JyInterpreter;
import com.macuyiko.minecraftpyserver.jython.JyWebSocketServer;
import com.macuyiko.minecraftpyserver.jython.TelnetServer;
import com.macuyiko.minecraftpyserver.py4j.PyCommandExecutor;

import py4j.ClientServer;
import py4j.GatewayServer;

public class MinecraftPyServerPlugin extends JavaPlugin {

	private static ClientServer py4jServer = null;
	@SuppressWarnings("unused")
	private static JyInterpreter pluginInterpreter = null;
	
	@Override
	public void onEnable() {
		log("Loading MinecraftPyServerPlugin");
		try {
			MinecraftPyServerUtils.setup();
			
			int tcpsocketserverport = getConfig().getInt("pythonconsole.serverconsole.telnetport", 44444);
			int websocketserverport = getConfig().getInt("pythonconsole.serverconsole.websocketport", 44445);
			int py4jport = getConfig().getInt("pythonconsole.serverconsole.py4jport", 25333);
			int httpport = getConfig().getInt("pythonconsole.serverconsole.httpeditorport", 8080);
			boolean enablechatcommands = getConfig().getString("pythonconsole.serverconsole.enablechatcommands", "true").equalsIgnoreCase("true");
			if (py4jport > 0)
				startPy4jServer(this, py4jport);
			if (tcpsocketserverport > 0)
				startTelnetServer(this, tcpsocketserverport);
			if (websocketserverport > 0)
				startWebSocketServer(this, websocketserverport);
			if (httpport > 0)
				JyHTTPServer.start(httpport, "lib-http");
			if (enablechatcommands) {
				JyChatServer commandServer = startChatServer(this);
				this.getCommand("jy").setExecutor(new JyCommandExecutor(this, commandServer));
				this.getCommand("jyrestart").setExecutor(new JyCommandExecutor(this, commandServer));
				this.getCommand("jyload").setExecutor(new JyCommandExecutor(this, commandServer));
				this.getCommand("pyrestart").setExecutor(new PyCommandExecutor(this));
				this.getCommand("pyload").setExecutor(new PyCommandExecutor(this));
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
	
	public static void startPy4jServer(MinecraftPyServerPlugin mainPlugin, int py4jport) {
		if (py4jServer != null) {
			py4jServer.shutdown();
		}
		
		py4jServer = new ClientServer(py4jport, GatewayServer.defaultAddress(), 
				py4jport + 1, GatewayServer.defaultAddress(), 
				GatewayServer.DEFAULT_CONNECT_TIMEOUT, GatewayServer.DEFAULT_READ_TIMEOUT, 
				ServerSocketFactory.getDefault(),  SocketFactory.getDefault(),
				null);
		py4jServer.startServer();
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
