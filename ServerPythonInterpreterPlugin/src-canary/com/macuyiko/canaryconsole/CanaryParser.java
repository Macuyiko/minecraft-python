package com.macuyiko.canaryconsole;

import org.python.util.InteractiveInterpreter;

public class CanaryParser {
	static public boolean parse(final InteractiveInterpreter interpreter, final String code, final boolean exec) {
		if (exec) {
			interpreter.exec(code);
			return false;
		}
		return interpreter.runsource(code);
	}
}
