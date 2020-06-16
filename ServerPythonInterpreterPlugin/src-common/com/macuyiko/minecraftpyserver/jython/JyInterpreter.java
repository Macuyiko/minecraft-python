package com.macuyiko.minecraftpyserver.jython;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.InteractiveInterpreter;

public class JyInterpreter extends InteractiveInterpreter {

	private static final AtomicInteger sequence = new AtomicInteger();
	private static final ConcurrentHashMap<Integer,JyInterpreter> interpreters = new ConcurrentHashMap<Integer,JyInterpreter>();
	
	private final int id;
	private long lastCall;
	private boolean permanent;
	private int timeout;
	private List<String> buffer = new ArrayList<String>();
	
	private static final int DEFAULT_IDLE_TIMEOUT = 60 * 15;
	
	public JyInterpreter() {
		this(false, DEFAULT_IDLE_TIMEOUT);
	}
	
	public JyInterpreter(boolean permanent) {
		this(permanent, DEFAULT_IDLE_TIMEOUT);
	}
	
	public JyInterpreter(int timeout) {
		this(false, timeout);
	}
	
	public JyInterpreter(boolean permanent, int timeout) {
		super(null, getPythonSystemState());
		this.id = sequence.incrementAndGet();
		interpreters.put(this.id, this);
		this.lastCall = System.currentTimeMillis();
		this.permanent = permanent;
		this.timeout = timeout;
		
		this.setOut(System.out);
		this.setErr(System.err);
	}
	
	public static void cleanIdle() {
		Iterator<JyInterpreter> it = interpreters.values().iterator();
		while (it.hasNext()) {
			JyInterpreter interpreter = it.next();
			if (!interpreter.isPermanent() && 
					interpreter.getTimeout() > 0 && 
					interpreter.getSecondsPassedSinceLastCall() >= interpreter.getTimeout()) {
				interpreter.close();
			}
		}
	}
	
	public boolean isAlive() {
		cleanIdle();
		return interpreters.containsKey(this.id);
	}
	
	public boolean isPermanent() {
		return permanent;
	}
	
	public void resetbuffer() {
		this.buffer.clear();
		super.resetbuffer();
	}

	public void close() {
		interpreters.remove(this.id);
		this.cleanup();
		super.close();
	}
	
	public int getSecondsPassedSinceLastCall() {
		return (int) ((System.currentTimeMillis() - this.lastCall) / 1000D);
	}
	
	public int getTimeout() {
		return timeout;
	}
	
	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		JyInterpreter other = (JyInterpreter) obj;
		if (id != other.id) return false;
		return true;
	}

	public boolean push(String line) {
		lastCall = System.currentTimeMillis();
		// The line should not have a trailing newline; it may have internal newlines.
		buffer.add(line);
		String source = String.join("\n", buffer);
		boolean more = this.runsource(source);
		if (!more)
			this.resetbuffer();
		return more;
	}
	
	public void exec(String code) {
		lastCall = System.currentTimeMillis();
		super.exec(code);
	}

	public void execfile(File script) {
		lastCall = System.currentTimeMillis();
		try {
			super.execfile(script.getAbsolutePath());
		} catch (PyException exc) {
            if (exc.match(Py.SystemExit)) {
                // Suppress this: we don't want clients to stop the whole JVM!
            	// We do stop this interpreter, however
            	this.close();
            	return;
            }
            showexception(exc);
        }
	}
	
	public void runcode(PyObject code) {
        try {
            exec(code);
        } catch (PyException exc) {
            if (exc.match(Py.SystemExit)) {
                // Suppress this: we don't want clients to stop the whole JVM!
            	// We do stop this interpreter, however
            	this.close();
            	return;
            }
            showexception(exc);
        }
    }
	
	public static PySystemState getPythonSystemState() {
		PySystemState sys = new PySystemState();
		addPathToPySystemState(sys, "./");
		addPathToPySystemState(sys, "./python/");
		addPathToPySystemState(sys, "./python-plugins/");
		File dependencyDirectory = new File("./");
		File[] files = dependencyDirectory.listFiles();
		for (int i = 0; i < files.length; i++) {
		    if (files[i].getName().toLowerCase().contains("spigot") && files[i].getName().toLowerCase().endsWith(".jar")) {
		    	addPathToPySystemState(sys, files[i].getAbsolutePath());
		    }
		}
		return sys;
	}
	
	public static void addPathToPySystemState(PySystemState sys, String path) {
		try {
			sys.path.append(new PyString(path));
		} catch (Exception e){}
	}
	
}
