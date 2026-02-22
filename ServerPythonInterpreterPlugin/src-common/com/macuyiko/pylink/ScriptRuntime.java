package com.macuyiko.pylink;

import java.nio.file.Path;

public interface ScriptRuntime extends AutoCloseable {

	void initialize() throws Exception;

	void executeEntrypoint(Path entrypoint) throws Exception;

	void invokeLoadHooks() throws Exception;

	void invokeUnloadHooks() throws Exception;

	@Override
	void close() throws Exception;
}
