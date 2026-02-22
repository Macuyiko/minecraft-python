package com.macuyiko.pylink;

import java.nio.file.Path;
import java.time.Instant;

public class ScriptInstance {

	private final String name;
	private final Path folder;
	private final Path entrypoint;
	private final ScriptRegistrationTracker registrationTracker;

	private ScriptRuntime runtime;
	private ScriptState state;
	private Instant loadedAt;

	public ScriptInstance(String name, Path folder, Path entrypoint) {
		this.name = name;
		this.folder = folder;
		this.entrypoint = entrypoint;
		this.registrationTracker = new ScriptRegistrationTracker();
		this.state = ScriptState.DISCOVERED;
	}

	public String getName() {
		return name;
	}

	public Path getFolder() {
		return folder;
	}

	public Path getEntrypoint() {
		return entrypoint;
	}

	public ScriptRegistrationTracker getRegistrationTracker() {
		return registrationTracker;
	}

	public ScriptRuntime getRuntime() {
		return runtime;
	}

	public void setRuntime(ScriptRuntime runtime) {
		this.runtime = runtime;
	}

	public ScriptState getState() {
		return state;
	}

	public void setState(ScriptState state) {
		this.state = state;
	}

	public Instant getLoadedAt() {
		return loadedAt;
	}

	public void markLoaded() {
		this.loadedAt = Instant.now();
		this.state = ScriptState.LOADED;
	}
}
