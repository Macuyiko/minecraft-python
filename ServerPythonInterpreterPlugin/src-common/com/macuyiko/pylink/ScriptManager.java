package com.macuyiko.pylink;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScriptManager {

	public static final String ENTRYPOINT_FILE = "main.py";

	private final Logger logger;
	private final ScriptRuntimeFactory runtimeFactory;
	private final Path scriptsDirectory;
	private final Map<String, ScriptInstance> loadedScripts = new LinkedHashMap<String, ScriptInstance>();

	public ScriptManager(Logger logger, ScriptRuntimeFactory runtimeFactory, Path scriptsDirectory) {
		this.logger = logger;
		this.runtimeFactory = runtimeFactory;
		this.scriptsDirectory = scriptsDirectory;
	}

	public Path getScriptsDirectory() {
		return scriptsDirectory;
	}

	public synchronized void ensureScriptsDirectoryExists() throws IOException {
		Files.createDirectories(scriptsDirectory);
	}

	public synchronized boolean isLoaded(String scriptName) {
		return loadedScripts.containsKey(scriptName);
	}

	public synchronized List<String> getLoadedScriptNames() {
		return new ArrayList<String>(loadedScripts.keySet());
	}

	public synchronized ScriptInstance getScript(String scriptName) {
		return loadedScripts.get(scriptName);
	}

	public synchronized List<String> discoverScriptFolders() throws IOException {
		if (!Files.exists(scriptsDirectory)) {
			return Collections.emptyList();
		}
		try (Stream<Path> stream = Files.list(scriptsDirectory)) {
			return stream.filter(Files::isDirectory).filter(path -> Files.exists(path.resolve(ENTRYPOINT_FILE)))
					.map(path -> path.getFileName().toString()).sorted().collect(Collectors.toList());
		}
	}

	public synchronized boolean load(String scriptName) throws Exception {
		if (isLoaded(scriptName)) {
			return false;
		}

		Path scriptFolder = scriptsDirectory.resolve(scriptName).normalize();
		Path entrypoint = scriptFolder.resolve(ENTRYPOINT_FILE).normalize();
		if (!Files.isDirectory(scriptFolder)) {
			throw new IOException("Script folder does not exist: " + scriptFolder);
		}
		if (!Files.isRegularFile(entrypoint)) {
			throw new IOException("Script entrypoint not found: " + entrypoint);
		}

		ScriptInstance script = new ScriptInstance(scriptName, scriptFolder, entrypoint);
		script.setState(ScriptState.LOADING);

		ScriptRuntime runtime = null;
		try {
			runtime = runtimeFactory.createRuntime(script);
			script.setRuntime(runtime);

			runtime.initialize();
			runtime.executeEntrypoint(entrypoint);
			runtime.invokeLoadHooks();
			script.markLoaded();
			loadedScripts.put(scriptName, script);
			logInfo("Loaded script: " + scriptName);
			return true;
		} catch (Exception e) {
			script.setState(ScriptState.FAILED);
			if (runtime != null) {
				try {
					runtime.close();
				} catch (Exception closeException) {
					logWarn("Failed to close runtime for script '" + scriptName + "': " + closeException.getMessage());
				}
			}
			throw e;
		}
	}

	public synchronized boolean unload(String scriptName) throws Exception {
		ScriptInstance script = loadedScripts.get(scriptName);
		if (script == null) {
			return false;
		}

		script.setState(ScriptState.UNLOADING);
		Exception unloadException = null;

		try {
			if (script.getRuntime() != null) {
				script.getRuntime().invokeUnloadHooks();
			}
		} catch (Exception e) {
			unloadException = e;
		}

		script.getRegistrationTracker().cleanupAll(logger, scriptName);

		try {
			if (script.getRuntime() != null) {
				script.getRuntime().close();
			}
		} catch (Exception closeException) {
			if (unloadException == null) {
				unloadException = closeException;
			} else {
				logWarn("Script '" + scriptName + "' close error after unload error: " + closeException.getMessage());
			}
		}

		loadedScripts.remove(scriptName);
		logInfo("Unloaded script: " + scriptName);

		if (unloadException != null) {
			throw unloadException;
		}
		return true;
	}

	public synchronized boolean reload(String scriptName) throws Exception {
		boolean wasLoaded = unload(scriptName);
		boolean loaded = load(scriptName);
		return wasLoaded || loaded;
	}

	public synchronized Map<String, Exception> loadAllDiscovered() {
		Map<String, Exception> failures = new LinkedHashMap<String, Exception>();
		List<String> discoveredScripts;
		try {
			discoveredScripts = discoverScriptFolders();
		} catch (Exception e) {
			failures.put("<discovery>", e);
			return failures;
		}

		for (String scriptName : discoveredScripts) {
			try {
				load(scriptName);
			} catch (Exception e) {
				failures.put(scriptName, e);
			}
		}

		return failures;
	}

	public synchronized Map<String, Exception> unloadAll() {
		Map<String, Exception> failures = new LinkedHashMap<String, Exception>();
		List<String> reverseOrder = new ArrayList<String>(loadedScripts.keySet());
		Collections.reverse(reverseOrder);
		for (String scriptName : reverseOrder) {
			try {
				unload(scriptName);
			} catch (Exception e) {
				failures.put(scriptName, e);
			}
		}
		return failures;
	}

	private void logInfo(String message) {
		if (logger != null) {
			logger.info(message);
		}
	}

	private void logWarn(String message) {
		if (logger != null) {
			logger.warning(message);
		}
	}
}
