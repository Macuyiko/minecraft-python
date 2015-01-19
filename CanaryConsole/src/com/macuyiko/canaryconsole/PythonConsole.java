package com.macuyiko.canaryconsole;

import java.io.File;

import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class PythonConsole {
	public PythonConsole() {
		PythonInterpreter interp = new PythonInterpreter(null, getPythonSystemState());
		String scriptname = "python/console.py";
		interp.execfile(scriptname);
	}
	
	public static PySystemState getPythonSystemState() {
		PySystemState sys = Py.getSystemState();
		sys.path.append(new PyString("."));
		sys.path.append(new PyString("python/"));

		try {
			File dependencyDirectory = new File("lib/");
			File[] files = dependencyDirectory.listFiles();
			for (int i = 0; i < files.length; i++) {
			    if (files[i].getName().endsWith(".jar")) {
			    	sys.path.append(new PyString(
			    			new File("lib/"+files[i].getName()).getAbsolutePath()));
			    }
			}
		} catch (Exception e){}
		
		return sys;
		
	}
}