package com.macuyiko.minecraftpyserver;

import org.python.util.InteractiveInterpreter;

public class CanaryParser {
	static public boolean parse(final InteractiveInterpreter interpreter, final String buffer) {
		return interpreter.runsource(buffer);
	}
}
