package com.macuyiko.pylink.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.macuyiko.pylink.PyLinkPlugin;
import com.macuyiko.pylink.ScriptManager;

public class PyLinkCommand implements CommandExecutor, TabCompleter {

	private static final List<String> SUBCOMMANDS = Arrays.asList("load", "unload", "reload", "list");

	private final PyLinkPlugin plugin;
	private final ScriptManager scriptManager;

	public PyLinkCommand(PyLinkPlugin plugin, ScriptManager scriptManager) {
		this.plugin = plugin;
		this.scriptManager = scriptManager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission("pylink.admin")) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
			return true;
		}

		if (args.length < 1 || args.length > 2) {
			sender.sendMessage(ChatColor.YELLOW + "Usage: /pylink load|unload|reload <script> | /pylink list");
			return true;
		}

		String action = args[0].toLowerCase(Locale.ROOT);
		try {
			if ("list".equals(action)) {
				if (args.length != 1) {
					sender.sendMessage(ChatColor.YELLOW + "Usage: /pylink list");
					return true;
				}
				List<String> loaded = scriptManager.getLoadedScriptNames();
				List<String> discovered = scriptManager.discoverScriptFolders();
				sender.sendMessage(ChatColor.AQUA + "Loaded scripts (" + loaded.size() + "): "
						+ (loaded.isEmpty() ? "-" : String.join(", ", loaded)));
				sender.sendMessage(ChatColor.AQUA + "Discovered scripts (" + discovered.size() + "): "
						+ (discovered.isEmpty() ? "-" : String.join(", ", discovered)));
				return true;
			}

			if (args.length != 2) {
				sender.sendMessage(ChatColor.YELLOW + "Usage: /pylink load|unload|reload <script>");
				return true;
			}
			String scriptName = args[1];

			if ("load".equals(action)) {
				boolean loaded = scriptManager.load(scriptName);
				sender.sendMessage(
						(loaded ? ChatColor.GREEN : ChatColor.YELLOW) + (loaded ? "Loaded " : "Already loaded: ")
								+ scriptName);
				return true;
			}
			if ("unload".equals(action)) {
				boolean unloaded = scriptManager.unload(scriptName);
				sender.sendMessage((unloaded ? ChatColor.GREEN : ChatColor.YELLOW)
						+ (unloaded ? "Unloaded " : "Not loaded: ") + scriptName);
				return true;
			}
			if ("reload".equals(action)) {
				scriptManager.reload(scriptName);
				sender.sendMessage(ChatColor.GREEN + "Reloaded " + scriptName);
				return true;
			}

			sender.sendMessage(ChatColor.YELLOW + "Unknown action '" + action + "'.");
			sender.sendMessage(ChatColor.YELLOW + "Usage: /pylink load|unload|reload <script> | /pylink list");
			return true;
		} catch (Exception e) {
			String scriptName = args.length > 1 ? args[1] : "<none>";
			Throwable root = rootCauseOf(e);
			String detail = messageOrType(root);
			plugin.getLogger().log(Level.SEVERE,
					"PyLink command failed for script '" + scriptName + "' (" + root.getClass().getSimpleName() + "): "
							+ detail,
					e);
			sender.sendMessage(ChatColor.RED + "Operation failed (" + root.getClass().getSimpleName() + "): " + detail);
			return true;
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!sender.hasPermission("pylink.admin")) {
			return Collections.emptyList();
		}

		if (args.length == 1) {
			return filterByPrefix(SUBCOMMANDS, args[0]);
		}
		if (args.length == 2) {
			try {
				String action = args[0].toLowerCase(Locale.ROOT);
				if ("load".equals(action) || "reload".equals(action)) {
					return filterByPrefix(scriptManager.discoverScriptFolders(), args[1]);
				}
				if ("unload".equals(action)) {
					return filterByPrefix(scriptManager.getLoadedScriptNames(), args[1]);
				}
			} catch (Exception e) {
				return Collections.emptyList();
			}
		}
		return Collections.emptyList();
	}

	private List<String> filterByPrefix(List<String> candidates, String prefix) {
		String expected = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
		List<String> results = new ArrayList<String>();
		for (String item : candidates) {
			if (item.toLowerCase(Locale.ROOT).startsWith(expected)) {
				results.add(item);
			}
		}
		return results;
	}

	private Throwable rootCauseOf(Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		return current;
	}

	private String messageOrType(Throwable throwable) {
		String message = throwable.getMessage();
		if (message == null || message.trim().isEmpty()) {
			return throwable.getClass().getName();
		}
		return message;
	}
}
