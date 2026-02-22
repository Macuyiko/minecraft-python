package com.macuyiko.pylink;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.macuyiko.pylink.command.PyLinkCommand;
import com.macuyiko.pylink.runtime.GraalPyScriptRuntimeFactory;

public class PyLinkPlugin extends JavaPlugin {

	public final static String PLUGIN_NAME = "pylink";
	private ScriptManager scriptManager;

	private Map<String, Supplier<Void>> onDisableFunctions = new HashMap<String, Supplier<Void>>();
	private ClassLoader pluginLoader;

	@Override
	public void onEnable() {
		log("Loading PyLink");

		pluginLoader = this.getClassLoader();
		saveDefaultConfig();
		PyLinkUtils.setup(pluginLoader, getLogger());
		String scriptDir = getConfig().getString("pylink.scriptsdir", "plugins/pylink/scripts");
		String librariesDir = getConfig().getString("pylink.librariesdir", "plugins/pylink/libraries");
		Path scriptsPath = Paths.get(scriptDir);
		Path librariesPath = Paths.get(librariesDir);
		scriptManager = new ScriptManager(getLogger(), new GraalPyScriptRuntimeFactory(this, getLogger(), librariesPath),
				scriptsPath);
		try {
			scriptManager.ensureScriptsDirectoryExists();
			Files.createDirectories(librariesPath);
		} catch (Exception e) {
			log("Failed to initialize scripts/libraries directory: " + e.getMessage());
		}
		PyLinkCommand pyLinkCommand = new PyLinkCommand(this, scriptManager);
		if (this.getCommand("pylink") != null) {
			this.getCommand("pylink").setExecutor(pyLinkCommand);
			this.getCommand("pylink").setTabCompleter(pyLinkCommand);
		}

		if (getConfig().getBoolean("pylink.autoload", true)) {
			startPyLinkScripts();
		}
	}

	public void onDisable() {
		log("Unloading PyLink");

		for (Entry<String, Supplier<Void>> disableFunc : onDisableFunctions.entrySet()) {
			log("Running disable function: " + disableFunc.getKey());
			try {
				disableFunc.getValue().get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		stopPyLinkScripts();
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

	public ClassLoader getPluginClassLoader() {
		return pluginLoader;
	}

	public ScriptManager getScriptManager() {
		return scriptManager;
	}

	private void startPyLinkScripts() {
		getServer().getScheduler().runTask(this, () -> {
			Map<String, Exception> failures = scriptManager.loadAllDiscovered();
			if (failures.isEmpty()) {
				log("PyLink autoload complete");
				return;
			}
			for (Entry<String, Exception> failure : failures.entrySet()) {
				Throwable root = rootCauseOf(failure.getValue());
				String detail = messageOrType(root);
				getLogger().log(Level.SEVERE,
						"PyLink autoload failed for '" + failure.getKey() + "' (" + root.getClass().getSimpleName()
								+ "): " + detail,
						failure.getValue());
			}
		});
	}

	private void stopPyLinkScripts() {
		if (scriptManager == null) {
			return;
		}
		Map<String, Exception> failures = scriptManager.unloadAll();
		for (Entry<String, Exception> failure : failures.entrySet()) {
			Throwable root = rootCauseOf(failure.getValue());
			String detail = messageOrType(root);
			getLogger().log(Level.SEVERE,
					"PyLink unload failed for '" + failure.getKey() + "' (" + root.getClass().getSimpleName() + "): "
							+ detail,
					failure.getValue());
		}
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
