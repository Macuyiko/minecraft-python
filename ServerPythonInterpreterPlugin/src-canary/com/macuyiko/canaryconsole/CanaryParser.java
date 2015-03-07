package com.macuyiko.canaryconsole;

import org.python.util.InteractiveInterpreter;

public class CanaryParser {
	static public boolean parse(final InteractiveInterpreter interpreter, final String code, final boolean exec) throws Exception {
		try {
			if (exec) {
				interpreter.exec(code);
				return false;
			}
			return interpreter.runsource(code);
		} catch (Throwable e) {
			throw new Exception(e);
		}
	}
}
