package com.macuyiko.bukkitconsole;

import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class PythonConsole {
	public PythonConsole() {
		PySystemState.initialize();

		PythonInterpreter interp = new PythonInterpreter(null,
				new PySystemState());

		PySystemState sys = Py.getSystemState();
		sys.path.append(new PyString("."));
		sys.path.append(new PyString("python/"));

		String scriptname = "python/console.py";
		interp.execfile(scriptname);
	}
}