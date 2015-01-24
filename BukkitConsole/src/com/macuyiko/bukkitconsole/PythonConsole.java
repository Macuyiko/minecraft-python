package com.macuyiko.bukkitconsole;

import java.io.File;

import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.InteractiveInterpreter;

public class PythonConsole {
	
	public PythonConsole() {
		InteractiveInterpreter interpeter = new InteractiveInterpreter(
						null, getPythonSystemState());
		String scriptname = "python/console.py";
		interpeter.execfile(scriptname);
	}
	
	public static PySystemState getPythonSystemState() {
		PySystemState sys = new PySystemState();
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