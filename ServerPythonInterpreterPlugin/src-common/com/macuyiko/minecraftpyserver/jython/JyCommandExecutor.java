package com.macuyiko.minecraftpyserver.jython;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

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
		if (!(sender instanceof Player))
			return false;
		Player player = (Player) sender;
		Permission p = new Permission("chatcommands", PermissionDefault.FALSE);
		if (!player.hasPermission(p)) {
			plugin.send(player, ChatColor.RED + "You don't have permission to use this command");
			return false;
		}
		
		if (cmd.getName().equals("py") && sender instanceof Player && args.length > 0) {
			String command = argsToString(args);
			plugin.send(player, ChatColor.AQUA + command);
			commandServer.command(player, command);
			return true;
		} else if (cmd.getName().equals("pyrestart") && sender instanceof Player) {
			plugin.send(player, "Restarting Python. Please wait...");
			commandServer.setupInterpreter(player);
			plugin.send(player, "Done!\n");
			return true;
		} else if (cmd.getName().equals("pyload") && sender instanceof Player && args.length == 1) {
			File match = MinecraftPyServerUtils.matchPythonFile(args[0]);
			if (match != null) {
				plugin.send(player, "Executing file: " + match.getName());
				commandServer.file(player, match);
				return true;
			} else {
				plugin.send(player, "Sorry, couldn't find this Python file");
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