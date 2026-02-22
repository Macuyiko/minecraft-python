package com.macuyiko.pylink.runtime;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.scheduler.BukkitTask;

import com.macuyiko.pylink.PyLinkPlugin;
import com.macuyiko.pylink.ScriptInstance;
import com.macuyiko.pylink.ScriptRuntime;

public class GraalPyScriptRuntime implements ScriptRuntime {

	private static final String PY_LANGUAGE_ID = "python";
	private static final String HOOK_LOAD = "__pylink_run_load_hooks()";
	private static final String HOOK_UNLOAD = "__pylink_run_unload_hooks()";

	private static final String BOOTSTRAP = ""
			+ "class _PyScriptLogger:\n"
			+ "    def __init__(self, script_name):\n"
			+ "        self._script_name = script_name\n"
			+ "    def info(self, message):\n"
			+ "        pylink_logger.info(str(message))\n"
			+ "    def warn(self, message):\n"
			+ "        pylink_logger.warn(str(message))\n"
			+ "    def error(self, message):\n"
			+ "        pylink_logger.error(str(message))\n"
			+ "    def debug(self, message):\n"
			+ "        pylink_logger.debug(str(message))\n"
			+ "\n"
			+ "_pylink_load_hooks = []\n"
			+ "_pylink_unload_hooks = []\n"
			+ "\n"
			+ "class _PyScript:\n"
			+ "    def __init__(self, name, folder, data_path):\n"
			+ "        self.name = name\n"
			+ "        self.folder = folder\n"
			+ "        self.data_path = data_path\n"
			+ "        self.logger = _PyScriptLogger(name)\n"
			+ "    def on_load(self, handler):\n"
			+ "        _pylink_load_hooks.append(handler)\n"
			+ "    def on_unload(self, handler):\n"
			+ "        _pylink_unload_hooks.append(handler)\n"
			+ "    def register_command(self, name, handler, aliases=None, permission=None, description=None, usage=None, tab_complete=None):\n"
			+ "        if aliases is None:\n"
			+ "            aliases = []\n"
			+ "        return pylink_host.registerCommand(name, handler, aliases, permission, description, usage, tab_complete)\n"
			+ "    def register_listener(self, event_class, handler, priority='NORMAL', ignore_cancelled=False):\n"
			+ "        return pylink_host.registerListener(event_class, handler, priority, ignore_cancelled)\n"
			+ "\n"
			+ "class _Scheduler:\n"
			+ "    def run(self, handler):\n"
			+ "        return pylink_scheduler.run(handler)\n"
			+ "    def run_delayed(self, handler, delay_ticks):\n"
			+ "        return pylink_scheduler.runDelayed(handler, int(delay_ticks))\n"
			+ "    def run_repeating(self, handler, delay_ticks, period_ticks):\n"
			+ "        return pylink_scheduler.runRepeating(handler, int(delay_ticks), int(period_ticks))\n"
			+ "    def run_async(self, handler):\n"
			+ "        return pylink_scheduler.runAsync(handler)\n"
			+ "    def run_delayed_async(self, handler, delay_ticks):\n"
			+ "        return pylink_scheduler.runDelayedAsync(handler, int(delay_ticks))\n"
			+ "    def run_repeating_async(self, handler, delay_ticks, period_ticks):\n"
			+ "        return pylink_scheduler.runRepeatingAsync(handler, int(delay_ticks), int(period_ticks))\n"
			+ "\n"
			+ "pyscript = _PyScript(pylink_name, pylink_folder, pylink_data_path)\n"
			+ "scheduler = _Scheduler()\n"
			+ "\n"
			+ "def __pylink_run_load_hooks():\n"
			+ "    for _fn in list(_pylink_load_hooks):\n"
			+ "        _fn()\n"
			+ "\n"
			+ "def __pylink_run_unload_hooks():\n"
			+ "    for _fn in list(_pylink_unload_hooks):\n"
			+ "        _fn()\n";

	private final PyLinkPlugin plugin;
	private final ScriptInstance script;
	private final Logger logger;
	private final Path librariesRoot;

	private Object context;
	private Method contextEvalMethod;
	private Method contextCloseMethod;
	private Method contextGetBindingsMethod;
	private Method valuePutMemberMethod;
	private Method valueExecuteMethod;
	private Method valueAsMethod;
	private Method valueAsValueStaticMethod;
	private Method valueHasArrayElementsMethod;
	private Method valueGetArraySizeMethod;
	private Method valueGetArrayElementMethod;
	private Method valueCanExecuteMethod;
	private CommandMap commandMap;

	public GraalPyScriptRuntime(PyLinkPlugin plugin, ScriptInstance script, Logger logger, Path librariesRoot) {
		this.plugin = plugin;
		this.script = script;
		this.logger = logger;
		this.librariesRoot = librariesRoot;
	}

	@Override
	public void initialize() throws Exception {
		try {
			Object builder = createContextBuilder();
			builder = callBuilderMethodIfPresent(builder, "allowAllAccess", boolean.class, true);
			builder = callBuilderMethodIfPresent(builder, "allowCreateThread", boolean.class, true);
			builder = callBuilderMethodIfPresent(builder, "allowIO", boolean.class, true);
			builder = callBuilderMethodIfPresent(builder, "option", String.class, String.class, "engine.WarnInterpreterOnly",
					"false");
			Method build = builder.getClass().getMethod("build");
			context = build.invoke(builder);
			Class<?> contextClass = Class.forName("org.graalvm.polyglot.Context");
			contextEvalMethod = contextClass.getMethod("eval", String.class, CharSequence.class);
			contextCloseMethod = contextClass.getMethod("close");
			contextGetBindingsMethod = contextClass.getMethod("getBindings", String.class);
			Class<?> valueClass = Class.forName("org.graalvm.polyglot.Value");
			valuePutMemberMethod = valueClass.getMethod("putMember", String.class, Object.class);
			valueExecuteMethod = valueClass.getMethod("execute", Object[].class);
			valueAsMethod = valueClass.getMethod("as", Class.class);
			valueAsValueStaticMethod = valueClass.getMethod("asValue", Object.class);
			valueHasArrayElementsMethod = valueClass.getMethod("hasArrayElements");
			valueGetArraySizeMethod = valueClass.getMethod("getArraySize");
			valueGetArrayElementMethod = valueClass.getMethod("getArrayElement", long.class);
			valueCanExecuteMethod = valueClass.getMethod("canExecute");
		} catch (ClassNotFoundException e) {
			throw new UnsupportedOperationException(
					"GraalVM polyglot classes not found. Add GraalPy/GraalVM jars to classpath.", e);
		}

		commandMap = resolveCommandMap();
		Files.createDirectories(script.getFolder().resolve("data"));
		Object bindings = contextGetBindingsMethod.invoke(context, PY_LANGUAGE_ID);
		putBinding(bindings, "pylink_name", script.getName());
		putBinding(bindings, "pylink_folder", script.getFolder().toAbsolutePath().toString());
		putBinding(bindings, "pylink_data_path", script.getFolder().resolve("data").toAbsolutePath().toString());
		putBinding(bindings, "pylink_logger", new PyScriptLoggerBridge());
		putBinding(bindings, "pylink_host", new PyScriptHostBridge());
		putBinding(bindings, "pylink_scheduler", new PyScriptSchedulerBridge());
		eval(BOOTSTRAP);
		appendDefaultPythonPaths();
	}

	private void appendDefaultPythonPaths() throws Exception {
		String scriptFolder = script.getFolder().toAbsolutePath().toString().replace("\\", "\\\\").replace("'", "\\'");
		String scriptsRoot = script.getFolder().getParent().toAbsolutePath().toString().replace("\\", "\\\\")
				.replace("'", "\\'");
		String libraries = librariesRoot == null ? null
				: librariesRoot.toAbsolutePath().toString().replace("\\", "\\\\").replace("'", "\\'");
		String code = "import sys\n" + "sys.path.insert(0, '" + scriptFolder + "')\n" + "sys.path.insert(0, '"
				+ scriptsRoot + "')\n";
		if (libraries != null && !libraries.isEmpty()) {
			code += "sys.path.insert(0, '" + libraries + "')\n";
		}
		eval(code);
	}

	private Object createContextBuilder() throws Exception {
		try {
			Class<?> resourcesClass = Class.forName("org.graalvm.python.embedding.GraalPyResources");
			Method contextBuilder = resourcesClass.getMethod("contextBuilder");
			return contextBuilder.invoke(null);
		} catch (ClassNotFoundException e) {
			Class<?> contextClass = Class.forName("org.graalvm.polyglot.Context");
			Method newBuilder = contextClass.getMethod("newBuilder", String[].class);
			return newBuilder.invoke(null, (Object) new String[] { PY_LANGUAGE_ID });
		} catch (InvocationTargetException e) {
			// GraalPyResources requires packaged VFS metadata (fileslist.txt). In plugin deployments
			// this metadata may be missing, so fall back to plain polyglot builder.
			Class<?> contextClass = Class.forName("org.graalvm.polyglot.Context");
			Method newBuilder = contextClass.getMethod("newBuilder", String[].class);
			return newBuilder.invoke(null, (Object) new String[] { PY_LANGUAGE_ID });
		} catch (IllegalStateException e) {
			Class<?> contextClass = Class.forName("org.graalvm.polyglot.Context");
			Method newBuilder = contextClass.getMethod("newBuilder", String[].class);
			return newBuilder.invoke(null, (Object) new String[] { PY_LANGUAGE_ID });
		}
	}

	@Override
	public void executeEntrypoint(Path entrypoint) throws Exception {
		String code;
		try {
			code = Files.readString(entrypoint, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IOException("Failed to read script entrypoint '" + entrypoint + "'", e);
		}
		eval(code);
	}

	@Override
	public void invokeLoadHooks() throws Exception {
		eval(HOOK_LOAD);
	}

	@Override
	public void invokeUnloadHooks() throws Exception {
		eval(HOOK_UNLOAD);
	}

	@Override
	public void close() throws Exception {
		if (context != null && contextCloseMethod != null) {
			contextCloseMethod.invoke(context);
			context = null;
		}
	}

	private Object callBuilderMethodIfPresent(Object builder, String methodName, Class<?> parameterType, Object arg)
			throws IllegalAccessException, InvocationTargetException {
		try {
			Method method = builder.getClass().getMethod(methodName, parameterType);
			return method.invoke(builder, arg);
		} catch (NoSuchMethodException e) {
			return builder;
		}
	}

	private Object callBuilderMethodIfPresent(Object builder, String methodName, Class<?> parameterTypeOne,
			Class<?> parameterTypeTwo, Object argOne, Object argTwo)
			throws IllegalAccessException, InvocationTargetException {
		try {
			Method method = builder.getClass().getMethod(methodName, parameterTypeOne, parameterTypeTwo);
			return method.invoke(builder, argOne, argTwo);
		} catch (NoSuchMethodException e) {
			return builder;
		}
	}

	private void eval(String source) throws Exception {
		if (context == null || contextEvalMethod == null) {
			throw new IllegalStateException("GraalPy context not initialized");
		}
		try {
			contextEvalMethod.invoke(context, PY_LANGUAGE_ID, source);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause() == null ? e : e.getCause();
			throw new RuntimeException(
					"Script '" + script.getName() + "' evaluation failed: " + cause.getMessage(), cause);
		}
	}

	private void putBinding(Object bindings, String key, Object value) throws Exception {
		valuePutMemberMethod.invoke(bindings, key, value);
	}

	private Object invokeCallable(Object callable, Object... args) throws Exception {
		if (callable == null) {
			throw new IllegalArgumentException("Callable is null");
		}
		Object callableValue = asPolyglotValue(callable);
		boolean canExecute = (Boolean) valueCanExecuteMethod.invoke(callableValue);
		if (!canExecute) {
			throw new IllegalArgumentException("Object is not executable");
		}
		try {
			return valueExecuteMethod.invoke(callableValue, new Object[] { args });
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause() == null ? e : e.getCause();
			throw new RuntimeException("Python callback execution failed: " + cause.getMessage(), cause);
		}
	}

	private Object asPolyglotValue(Object value) throws Exception {
		if (value == null) {
			return null;
		}
		if (valueCanExecuteMethod.getDeclaringClass().isInstance(value)) {
			return value;
		}
		return valueAsValueStaticMethod.invoke(null, value);
	}

	private String normalizeString(Object value) {
		if (value == null) {
			return null;
		}
		String stringValue = String.valueOf(value).trim();
		return stringValue.isEmpty() ? null : stringValue;
	}

	private List<String> normalizeStringList(Object value) {
		if (value == null) {
			return Collections.emptyList();
		}
		List<String> values = new ArrayList<String>();
		try {
			Object polyglotValue = asPolyglotValue(value);
			boolean hasArrayElements = (Boolean) valueHasArrayElementsMethod.invoke(polyglotValue);
			if (hasArrayElements) {
				long size = (Long) valueGetArraySizeMethod.invoke(polyglotValue);
				for (long index = 0; index < size; index++) {
					Object element = valueGetArrayElementMethod.invoke(polyglotValue, index);
					String normalized = normalizeString(element);
					if (normalized != null) {
						values.add(normalized);
					}
				}
				return values;
			}
		} catch (Exception e) {
			// Fallback below.
		}
		if (value instanceof List<?>) {
			for (Object element : (List<?>) value) {
				String normalized = normalizeString(element);
				if (normalized != null) {
					values.add(normalized);
				}
			}
			return values;
		}
		String single = normalizeString(value);
		return single == null ? Collections.emptyList() : Collections.singletonList(single);
	}

	@SuppressWarnings("unchecked")
	private List<String> normalizeCompletionResult(Object value) {
		if (value == null) {
			return Collections.emptyList();
		}
		try {
			List<String> list = (List<String>) valueAsMethod.invoke(value, List.class);
			if (list != null) {
				return list;
			}
		} catch (Exception e) {
			// Fallback below.
		}
		return normalizeStringList(value);
	}

	private EventPriority parsePriority(String priorityValue) {
		if (priorityValue == null) {
			return EventPriority.NORMAL;
		}
		try {
			return EventPriority.valueOf(priorityValue.trim().toUpperCase(Locale.ROOT));
		} catch (Exception e) {
			return EventPriority.NORMAL;
		}
	}

	private CommandMap resolveCommandMap() {
		PluginManager manager = plugin.getServer().getPluginManager();
		if (!(manager instanceof SimplePluginManager)) {
			throw new IllegalStateException("Unsupported plugin manager implementation: " + manager.getClass().getName());
		}
		try {
			Class<?> pluginManagerClass = manager.getClass();
			java.lang.reflect.Field commandMapField = pluginManagerClass.getDeclaredField("commandMap");
			commandMapField.setAccessible(true);
			return (CommandMap) commandMapField.get(manager);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to resolve Bukkit command map", e);
		}
	}

	private void logWarning(String message) {
		if (logger != null) {
			logger.warning("[" + script.getName() + "] " + message);
		}
	}

	private void logScriptMessage(String level, Object message) {
		if (logger != null) {
			logger.info("[" + script.getName() + "][" + level + "] " + String.valueOf(message));
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

	@SuppressWarnings("unchecked")
	private Map<String, Command> getKnownCommandsMap() {
		Class<?> type = commandMap.getClass();
		while (type != null) {
			try {
				java.lang.reflect.Field knownCommandsField = type.getDeclaredField("knownCommands");
				knownCommandsField.setAccessible(true);
				Object value = knownCommandsField.get(commandMap);
				if (value instanceof Map<?, ?>) {
					return (Map<String, Command>) value;
				}
				return null;
			} catch (NoSuchFieldException e) {
				type = type.getSuperclass();
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	private void purgeStaleScriptCommandEntries(String commandName, List<String> aliases) {
		Map<String, Command> knownCommands = getKnownCommandsMap();
		if (knownCommands == null) {
			return;
		}

		String prefix = plugin.getDescription().getName().toLowerCase(Locale.ROOT);
		Set<String> keysToCheck = new HashSet<String>();
		String normalizedName = commandName.toLowerCase(Locale.ROOT);
		keysToCheck.add(normalizedName);
		keysToCheck.add(prefix + ":" + normalizedName);
		for (String alias : aliases) {
			String normalizedAlias = alias.toLowerCase(Locale.ROOT);
			keysToCheck.add(normalizedAlias);
			keysToCheck.add(prefix + ":" + normalizedAlias);
		}

		for (String key : keysToCheck) {
			Command existing = knownCommands.get(key);
			if (existing instanceof ScriptCommand) {
				try {
					knownCommands.remove(key);
				} catch (Exception ignored) {
					// Best-effort cleanup.
				}
			}
		}
	}

	private void removeCommandReferences(Map<String, Command> knownCommands, Command target) {
		List<String> keysToRemove = new ArrayList<String>();
		for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
			if (entry.getValue() == target) {
				keysToRemove.add(entry.getKey());
			}
		}
		for (String key : keysToRemove) {
			try {
				knownCommands.remove(key);
			} catch (Exception ignored) {
				// Best-effort cleanup.
			}
		}
	}

	public final class PyScriptHostBridge {

		public ScriptCommandHandle registerCommand(Object name, Object handler, Object aliases, Object permission,
				Object description, Object usage, Object tabComplete) throws Exception {
			String commandName = normalizeString(name);
			if (commandName == null) {
				throw new IllegalArgumentException("Command name is required");
			}
			if (handler == null) {
				throw new IllegalArgumentException("Command handler is required");
			}

			ScriptCommand command = new ScriptCommand(commandName, handler, tabComplete);
			command.setAliases(normalizeStringList(aliases));
			String permissionValue = normalizeString(permission);
			if (permissionValue != null) {
				command.setPermission(permissionValue);
			}
			String descriptionValue = normalizeString(description);
			if (descriptionValue != null) {
				command.setDescription(descriptionValue);
			}
			String usageValue = normalizeString(usage);
			if (usageValue != null) {
				command.setUsage(usageValue);
			}

			purgeStaleScriptCommandEntries(commandName, command.getAliases());
			boolean registered = commandMap.register(plugin.getDescription().getName(), command);
			if (!registered) {
				throw new IllegalStateException("Command registration failed for '" + commandName + "'");
			}

			ScriptCommandHandle handle = new ScriptCommandHandle(commandMap, command);
			script.getRegistrationTracker().track(handle);
			return handle;
		}

		@SuppressWarnings({ "unchecked" })
		public ScriptListenerHandle registerListener(Object eventClass, Object handler, Object priority,
				Object ignoreCancelled) throws Exception {
			String eventClassName = normalizeString(eventClass);
			if (eventClassName == null) {
				throw new IllegalArgumentException("Event class is required");
			}
			if (handler == null) {
				throw new IllegalArgumentException("Listener handler is required");
			}

			Class<?> clazz = Class.forName(eventClassName);
			if (!Event.class.isAssignableFrom(clazz)) {
				throw new IllegalArgumentException("Not a Bukkit Event class: " + eventClassName);
			}

			Listener listener = new Listener() {
			};
			EventPriority eventPriority = parsePriority(normalizeString(priority));
			boolean ignore = ignoreCancelled != null && Boolean.parseBoolean(String.valueOf(ignoreCancelled));
			EventExecutor executor = new EventExecutor() {
				@Override
				public void execute(Listener ignoredListener, Event event) throws EventException {
					try {
						invokeCallable(handler, event);
					} catch (Exception e) {
						if (logger != null) {
							Throwable root = rootCauseOf(e);
							logger.log(Level.WARNING,
									"[" + script.getName() + "] Listener callback failed for " + eventClassName + " ("
											+ root.getClass().getSimpleName() + "): " + messageOrType(root),
									e);
						}
					}
				}
			};

			plugin.getServer().getPluginManager().registerEvent((Class<? extends Event>) clazz, listener, eventPriority,
					executor, plugin, ignore);

			ScriptListenerHandle handle = new ScriptListenerHandle(listener);
			script.getRegistrationTracker().track(handle);
			return handle;
		}
	}

	public final class PyScriptLoggerBridge {

		public void info(Object message) {
			logScriptMessage("INFO", message);
		}

		public void warn(Object message) {
			logScriptMessage("WARN", message);
		}

		public void error(Object message) {
			logScriptMessage("ERROR", message);
		}

		public void debug(Object message) {
			logScriptMessage("DEBUG", message);
		}
	}

	public final class PyScriptSchedulerBridge {

		public ScriptTaskHandle run(Object handler) {
			return schedule(handler, 0L, 0L, false, false);
		}

		public ScriptTaskHandle runDelayed(Object handler, int delayTicks) {
			return schedule(handler, Math.max(0, delayTicks), 0L, false, false);
		}

		public ScriptTaskHandle runRepeating(Object handler, int delayTicks, int periodTicks) {
			return schedule(handler, Math.max(0, delayTicks), Math.max(1, periodTicks), true, false);
		}

		public ScriptTaskHandle runAsync(Object handler) {
			return schedule(handler, 0L, 0L, false, true);
		}

		public ScriptTaskHandle runDelayedAsync(Object handler, int delayTicks) {
			return schedule(handler, Math.max(0, delayTicks), 0L, false, true);
		}

		public ScriptTaskHandle runRepeatingAsync(Object handler, int delayTicks, int periodTicks) {
			return schedule(handler, Math.max(0, delayTicks), Math.max(1, periodTicks), true, true);
		}

		private ScriptTaskHandle schedule(Object handler, long delayTicks, long periodTicks, boolean repeating,
				boolean async) {
			if (handler == null) {
				throw new IllegalArgumentException("Scheduler handler is required");
			}

			ScriptTaskHandle handle = new ScriptTaskHandle();
			Runnable runnable = () -> {
				try {
					invokeCallable(handler);
				} catch (Exception e) {
					logWarning("Scheduled callback failed: " + e.getMessage());
				}
			};

			BukkitTask task;
			if (async) {
				if (repeating) {
					task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
				} else if (delayTicks > 0) {
					task = plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks);
				} else {
					task = plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
				}
			} else {
				if (repeating) {
					task = plugin.getServer().getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
				} else if (delayTicks > 0) {
					task = plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delayTicks);
				} else {
					task = plugin.getServer().getScheduler().runTask(plugin, runnable);
				}
			}

			handle.setTask(task);
			script.getRegistrationTracker().track(handle);
			return handle;
		}
	}

	public final class ScriptCommandHandle implements AutoCloseable {

		private final CommandMap commandMap;
		private final ScriptCommand command;
		private final AtomicBoolean closed = new AtomicBoolean(false);

		public ScriptCommandHandle(CommandMap commandMap, ScriptCommand command) {
			this.commandMap = commandMap;
			this.command = command;
		}

		public void unregister() {
			if (!closed.compareAndSet(false, true)) {
				return;
			}
			command.unregister(commandMap);
			Map<String, Command> knownCommands = getKnownCommandsMap();
			if (knownCommands != null) {
				removeCommandReferences(knownCommands, command);
			}
		}

		@Override
		public void close() {
			unregister();
		}
	}

	public static final class ScriptListenerHandle implements AutoCloseable {

		private final Listener listener;
		private final AtomicBoolean closed = new AtomicBoolean(false);

		public ScriptListenerHandle(Listener listener) {
			this.listener = listener;
		}

		public void unregister() {
			if (closed.compareAndSet(false, true)) {
				HandlerList.unregisterAll(listener);
			}
		}

		@Override
		public void close() {
			unregister();
		}
	}

	public static final class ScriptTaskHandle implements AutoCloseable {

		private BukkitTask task;
		private final AtomicBoolean closed = new AtomicBoolean(false);

		private void setTask(BukkitTask task) {
			this.task = task;
		}

		public void cancel() {
			if (!closed.compareAndSet(false, true)) {
				return;
			}
			if (task != null) {
				task.cancel();
			}
		}

		@Override
		public void close() {
			cancel();
		}
	}

	private final class ScriptCommand extends Command {

		private final Object handler;
		private final Object tabComplete;

		private ScriptCommand(String name, Object handler, Object tabComplete) {
			super(name);
			this.handler = handler;
			this.tabComplete = tabComplete;
		}

		@Override
		public boolean execute(CommandSender sender, String commandLabel, String[] args) {
			if (!testPermission(sender)) {
				return true;
			}
			try {
				invokeCallable(handler, sender, args);
				return true;
			} catch (Exception e) {
				logWarning("Command callback failed for '" + getName() + "': " + e.getMessage());
				sender.sendMessage("An error occurred in script command '" + getName() + "'.");
				return true;
			}
		}

		@Override
		public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
			if (tabComplete == null) {
				return Collections.emptyList();
			}
			try {
				Object result = invokeCallable(tabComplete, sender, args);
				return normalizeCompletionResult(result);
			} catch (Exception e) {
				return Collections.emptyList();
			}
		}
	}
}
