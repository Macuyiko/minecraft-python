package com.macuyiko.canaryconsole;

import org.python.util.InteractiveInterpreter;

public class CanaryParser {
<<<<<<< HEAD
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
=======
	static public boolean parse(final InteractiveInterpreter interpreter, final String code, final boolean exec) {
		if (exec) {
			interpreter.exec(code);
			return false;
		}
		return interpreter.runsource(code);
>>>>>>> e6993e06840a2fa75ba9215a42517c98faba1526
	}
}
