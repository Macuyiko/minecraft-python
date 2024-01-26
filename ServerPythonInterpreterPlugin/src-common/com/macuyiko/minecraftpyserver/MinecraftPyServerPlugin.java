package com.macuyiko.minecraftpyserver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.macuyiko.minecraftpyserver.jython.JyChatServer;
import com.macuyiko.minecraftpyserver.jython.JyCommandExecutor;
import com.macuyiko.minecraftpyserver.jython.JyCommandListener;
import com.macuyiko.minecraftpyserver.jython.JyInterpreter;
import com.macuyiko.minecraftpyserver.jython.JyTelnetServer;
import com.macuyiko.minecraftpyserver.jython.JyWebSocketServer;
import com.macuyiko.minecraftpyserver.py4j.Py4JServer;

public class MinecraftPyServerPlugin extends JavaPlugin {

	public final static String PLUGIN_NAME = "MinecraftPyServer";

	private List<JyInterpreter> pluginInterpreters = new ArrayList<JyInterpreter>();
	private JyWebSocketServer webSocketServer;
	private JyTelnetServer telnetServer;
	private JyChatServer commandServer;
	private Py4JServer py4JServer;
	private Thread telnetServerThread;
	private Thread py4JServerThread;

	private Map<String, Supplier<Void>> onDisableFunctions = new HashMap<String, Supplier<Void>>();
	private ClassLoader pluginLoader;

	@Override
	public void onEnable() {
		log("Loading MinecraftPyServerPlugin");

		pluginLoader = this.getClassLoader();
		MinecraftPyServerUtils.setup(pluginLoader, getLogger());

		int telnetPort = getConfig().getInt("pythonconsole.telnetport", 44444);
		int py4JPort = getConfig().getInt("pythonconsole.telnetport", 44448);
		int websocketPort = getConfig().getInt("pythonconsole.websocketport", 44445);
		boolean enableChat = getConfig().getString("pythonconsole.enablechatcommands", "true").equalsIgnoreCase("true");

		if (telnetPort > 0)
			telnetServer = startTelnetServer(this, telnetPort);

		if (py4JPort > 0)
			py4JServer = startPy4JServer(this, py4JPort);

		if (websocketPort > 0)
			webSocketServer = startWebSocketServer(this, websocketPort);

		if (enableChat) {
			commandServer = startChatServer(this);
			this.getCommand("py").setExecutor(new JyCommandExecutor(this, commandServer));
			this.getCommand("pyrestart").setExecutor(new JyCommandExecutor(this, commandServer));
			this.getCommand("pyload").setExecutor(new JyCommandExecutor(this, commandServer));
			this.getCommand("pyreload").setExecutor(new JyCommandExecutor(this, commandServer));
		}

		getServer().getPluginManager().registerEvents(new JyCommandListener(), this);

		startPluginInterpreters();
	}

	public void onDisable() {
		log("Unloading MinecraftPyServerPlugin");

		for (Entry<String, Supplier<Void>> disableFunc : onDisableFunctions.entrySet()) {
			log("Running disable function: " + disableFunc.getKey());
			try {
				disableFunc.getValue().get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		stopPluginInterpreters();

		if (webSocketServer != null) {
			try {
				webSocketServer.stop();
			} catch (IOException e) {
			} catch (InterruptedException e) {
			}
		}

		if (telnetServer != null) {
			telnetServerThread.interrupt();
			try {
				telnetServerThread.join(1000);
			} catch (InterruptedException e) {
			}
			telnetServer.close();
		}

		if (py4JServer != null) {
			py4JServerThread.interrupt();
			try {
				py4JServerThread.join(1000);
			} catch (InterruptedException e) {
			}
			py4JServer.close();
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

	public void registerOnDisableHandler(String name, Supplier<Void> function) {
		onDisableFunctions.put(name, function);
	}

	public void unregisterOnDisableHandler(String name) {
		onDisableFunctions.remove(name);
	}

	public void restartPluginInterpreters() {
		stopPluginInterpreters();
		startPluginInterpreters();
	}

	public ClassLoader getPluginClassLoader() {
		return pluginLoader;
	}

	private void startPluginInterpreters() {
		File pluginDirectory = new File("./python-plugins/");
		if (pluginDirectory.exists() && pluginDirectory.isDirectory()) {
			File[] files = pluginDirectory.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].getName().endsWith(".py")) {
					log("Parsing plugin: " + files[i].getName());
					JyInterpreter pluginInterpreter = new JyInterpreter(getPluginClassLoader(), true);
					pluginInterpreter.execfile(files[i]);
					pluginInterpreters.add(pluginInterpreter);
				}
			}
		}
	}

	private void stopPluginInterpreters() {
		for (JyInterpreter pluginInterpreter : pluginInterpreters) {
			pluginInterpreter.close();
		}
	}

	private JyTelnetServer startTelnetServer(MinecraftPyServerPlugin mainPlugin, int port) {
		JyTelnetServer server = new JyTelnetServer(mainPlugin, port);
		telnetServerThread = new Thread(server);
		telnetServerThread.start();
		return server;
	}

	private Py4JServer startPy4JServer(MinecraftPyServerPlugin mainPlugin, int port) {
		Py4JServer server = new Py4JServer(mainPlugin, port);
		py4JServerThread = new Thread(server);
		py4JServerThread.start();
		return server;
	}

	private JyWebSocketServer startWebSocketServer(MinecraftPyServerPlugin mainPlugin, int port) {
		JyWebSocketServer server = new JyWebSocketServer(mainPlugin, port);
		server.start();
		return server;
	}

	private JyChatServer startChatServer(MinecraftPyServerPlugin mainPlugin) {
		JyChatServer server = new JyChatServer(mainPlugin);
		return server;
	}

}
