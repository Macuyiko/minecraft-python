package com.macuyiko.minecraftpyserver.jython;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.macuyiko.minecraftpyserver.MinecraftPyServerPlugin;
import com.macuyiko.minecraftpyserver.MinecraftPyServerUtils;

public class JyCommandExecutor implements CommandExecutor {
	private JyChatServer commandServer;
	private MinecraftPyServerPlugin plugin;

	public JyCommandExecutor(MinecraftPyServerPlugin plugin, JyChatServer commandServer) {
		this.plugin = plugin;
		this.commandServer = commandServer;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equals("jy") && sender instanceof Player && args.length > 0) {
			Player player = (Player) sender;
			String command = argsToString(args);
			plugin.send(player.getDisplayName(), ChatColor.AQUA + command);
			commandServer.command(player.getDisplayName(), command);
			return true;
		} else if (cmd.getName().equals("jyrestart") && sender instanceof Player) {
			Player player = (Player) sender;
			plugin.send(player.getDisplayName(), "Restarting Python. Please wait...");
			commandServer.setupInterpreter(player.getDisplayName());
			plugin.send(player.getDisplayName(), "Done!\n");
			return true;
		} else if (cmd.getName().equals("jyload") && sender instanceof Player && args.length == 1) {
			Player player = (Player) sender;
			File match = MinecraftPyServerUtils.matchPythonFile(args[0]);
			if (match != null) {
				plugin.send(player.getDisplayName(), "Executing file: " + match.getName());
				commandServer.file(player.getDisplayName(), match);
				return true;
			} else {
				plugin.send(player.getDisplayName(), "Sorry, couldn't find this Python file");
			}
		}
		return false;
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