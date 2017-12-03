package com.macuyiko.minecraftpyserver;

import java.io.File;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.python.core.PyException;

import com.macuyiko.minecraftpyserver.servers.PyChatServer;
import com.macuyiko.minecraftpyserver.servers.PyTelnetServer;
import com.macuyiko.minecraftpyserver.servers.PyWebSocketServer;

import py4j.ClientServer;

public class SpigotPlugin extends JavaPlugin implements PyPlugin {

	@Override
	public void onEnable() {
		log("Loading Python Console");
		try {
			SetupUtils.setup();
			int tcpsocketserverport = getConfig().getInt("pythonconsole.serverconsole.telnetport", 44444);
			int websocketserverport = getConfig().getInt("pythonconsole.serverconsole.websocketport", 44445);
			int py4jport = getConfig().getInt("pythonconsole.serverconsole.py4jport", 25333);
			boolean enablechatcommands = getConfig().getString("pythonconsole.serverconsole.enablechatcommands", "true").equalsIgnoreCase("true");
			if (py4jport > 0)
				startPy4jServer(this, py4jport);
			if (tcpsocketserverport > 0)
				startTelnetServer(this, tcpsocketserverport);
			if (websocketserverport > 0)
				startWebSocketServer(this, websocketserverport);
			if (enablechatcommands) {
				PyChatServer commandServer = startChatServer(this);
				this.getCommand("py").setExecutor(new CommandPy(commandServer));
				this.getCommand("pyrestart").setExecutor(new CommandPy(commandServer));
				this.getCommand("pyload").setExecutor(new CommandPy(commandServer));
			}
		} catch (IOException e) {
			getLogger().severe(e.getMessage());
			return;
		}
	}
	
	@Override
	public void log(String message) {
		getLogger().info(message);
	}

	public void send(String player, String message) {
		Player p = getServer().getPlayer(player);
		p.sendMessage(ChatColor.GREEN + message.replace("\r", ""));
		
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

	public static PyTelnetServer startTelnetServer(PyPlugin mainPlugin, int telnetport) {
		PyTelnetServer server = new PyTelnetServer(mainPlugin, telnetport);
		Thread t = new Thread(server);
		t.start();
		return server;
	}
	
	public static void startPy4jServer(PyPlugin mainPlugin, int py4jport) {
		ClientServer server = new ClientServer(null);
		server.startServer();
	}
	
	public static PyWebSocketServer startWebSocketServer(PyPlugin mainPlugin, int websocketport) {
		PyWebSocketServer server = new PyWebSocketServer(mainPlugin, websocketport);
		server.start();
		return server;
	}
	
	public static PyChatServer startChatServer(PyPlugin mainPlugin) {
		PyChatServer server = new PyChatServer(mainPlugin);
		return server;
	}

	public class CommandPy implements CommandExecutor {
		private PyChatServer commandServer;

		public CommandPy(PyChatServer commandServer) {
			this.commandServer = commandServer;
		}

		@Override
		public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
			if (cmd.getName().equals("py") && sender instanceof Player && args.length > 0) {
				Player player = (Player) sender;
				String command = argsToString(args);
				send(player.getDisplayName(), ChatColor.AQUA + command);
				commandServer.command(player.getDisplayName(), command);
				return true;
			} else if (cmd.getName().equals("pyrestart") && sender instanceof Player) {
				Player player = (Player) sender;
				send(player.getDisplayName(), "Restarting Python. Please wait...");
				commandServer.setupInterpreter(player.getDisplayName());
				send(player.getDisplayName(), "Done!\n");
				return true;
			} else if (cmd.getName().equals("pyload") && sender instanceof Player && args.length == 1) {
				Player player = (Player) sender;
				File match = matchPythonFile(args[0]);
				if (match != null) {
					send(player.getDisplayName(), "Executing file: " + match.getName());
					commandServer.file(player.getDisplayName(), match);
					return true;
				} else {
					send(player.getDisplayName(), "Sorry, couldn't find this Python file");
				}
			}
			return false;
		}

	}
	
	public static File matchPythonFile(String arg) {
		File asIs = new File(arg);
		File onDesktop = new File(javax.swing.filechooser.FileSystemView.getFileSystemView()
				.getHomeDirectory().getAbsolutePath(), arg);
		File asIsPy = new File(arg + ".py");
		File onDesktopPy = new File(javax.swing.filechooser.FileSystemView.getFileSystemView()
				.getHomeDirectory().getAbsolutePath(), arg + ".py");
		if (asIs.exists()) return asIs;
		if (onDesktop.exists()) return onDesktop;
		if (asIsPy.exists()) return asIsPy;
		if (onDesktopPy.exists()) return onDesktopPy;
		return null;
	}

	public static String argsToString(String[] args) {
		String myString = "";
		for (int i = 0; i < args.length; i++) {
			if (i == 0 && args[i].equals(".")) {
				myString = myString + " ";
				continue;
			}
			myString = myString + args[i] + " ";
		}
		myString = myString.replaceAll("\\s+$","");
		return myString;
	}
}
