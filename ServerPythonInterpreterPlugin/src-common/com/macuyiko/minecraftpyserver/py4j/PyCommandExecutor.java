package com.macuyiko.minecraftpyserver.py4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.macuyiko.minecraftpyserver.MinecraftPyServerPlugin;
import com.macuyiko.minecraftpyserver.MinecraftPyServerUtils;

public class PyCommandExecutor implements CommandExecutor {

	private MinecraftPyServerPlugin plugin;

	public PyCommandExecutor(MinecraftPyServerPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equals("pyrestart") && sender instanceof Player) {
			// TODO: Improve this -- ugly to query for port number twice
			int py4jport = plugin.getConfig().getInt("pythonconsole.serverconsole.py4jport", 25333);
			MinecraftPyServerPlugin.startPy4jServer(plugin, py4jport);
			return true;
		} else if (cmd.getName().equals("pyload") && sender instanceof Player && args.length == 1) {
			Player player = (Player) sender;
			File match = MinecraftPyServerUtils.matchPythonFile(args[0]);
			if (match != null) {
				plugin.send(player.getDisplayName(), "Executing file: " + match.getName());
				file(player.getDisplayName(), match);
				return true;
			} else {
				plugin.send(player.getDisplayName(), "Sorry, couldn't find this Python file");
			}
		}
		return false;
	}

	public void file(String player, File script) {
		try {
			Process p = Runtime.getRuntime().exec("python " + script.getAbsolutePath());
			StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), player);
			StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), player);
			errorGobbler.start();
			outputGobbler.start();
			if(!p.waitFor(10, TimeUnit.MINUTES)) {
			    p.destroyForcibly();
			    plugin.send(player, "Python process killed after timeout");
			}
		} catch (IOException e) {
			plugin.send(player, "Could not start Python on the server");
		} catch (InterruptedException e) {
			plugin.send(player, "Python process interrupted");
		}
	}
	
	class StreamGobbler extends Thread {
		private InputStream is;
		private String player;
		
		public StreamGobbler(InputStream is, String player) {
			this.is = is;
			this.player = player;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null)
					plugin.send(player, line);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

}