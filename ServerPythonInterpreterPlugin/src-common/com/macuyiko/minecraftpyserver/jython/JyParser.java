package com.macuyiko.minecraftpyserver.jython;

import java.io.File;

import org.python.core.PyException;

public class JyParser {
	public static boolean parse(final JyInterpreter interpreter, final String code, final boolean exec) throws PyException {
		final JyInterpreterRunnable runnable = new JyInterpreterRunnable(interpreter, code, exec);
		runnable.run();
		return runnable.more();
	}

	public static boolean parse(final JyInterpreter interpreter, final File script) throws PyException {
		final JyInterpreterRunnable runnable = new JyInterpreterRunnable(interpreter, script);
		runnable.run();
		return runnable.more();
	}

	
}
