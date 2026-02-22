package com.macuyiko.pylink.runtime;

import java.nio.file.Path;
import java.util.logging.Logger;

import com.macuyiko.pylink.PyLinkPlugin;
import com.macuyiko.pylink.ScriptInstance;
import com.macuyiko.pylink.ScriptRuntime;
import com.macuyiko.pylink.ScriptRuntimeFactory;

public class GraalPyScriptRuntimeFactory implements ScriptRuntimeFactory {

	private final Logger logger;
	private final PyLinkPlugin plugin;
	private final Path librariesRoot;

	public GraalPyScriptRuntimeFactory(PyLinkPlugin plugin, Logger logger, Path librariesRoot) {
		this.plugin = plugin;
		this.logger = logger;
		this.librariesRoot = librariesRoot;
	}

	@Override
	public ScriptRuntime createRuntime(ScriptInstance script) throws Exception {
		return new GraalPyScriptRuntime(plugin, script, logger, librariesRoot);
	}

}
