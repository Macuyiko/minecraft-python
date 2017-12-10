package com.macuyiko.minecraftpyserver.rembulan;

import java.io.File;

import org.python.core.PyException;

public class LuaParser {
	public static boolean parse(final RembulanInterpreter interpreter, final String code) throws PyException {
		final RembulanInterpreterRunnable runnable = new RembulanInterpreterRunnable(interpreter, code);
		runnable.run();
		return runnable.more();
	}

	public static boolean parse(final RembulanInterpreter interpreter, final File script) throws PyException {
		final RembulanInterpreterRunnable runnable = new RembulanInterpreterRunnable(interpreter, script);
		runnable.run();
		return runnable.more();
	}

	
}
