package com.macuyiko.bukkitconsole;

import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class PythonConsole {
	public PythonConsole() {
		PySystemState sys = Py.getSystemState();
		sys.path.append(new PyString("."));
		sys.path.append(new PyString("python/"));

		PythonInterpreter interp = new PythonInterpreter(null, sys);

		String scriptname = "python/console.py";
		interp.execfile(scriptname);
		interp.close();
	}
}