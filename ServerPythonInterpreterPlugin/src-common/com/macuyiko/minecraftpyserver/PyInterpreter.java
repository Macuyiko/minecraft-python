package com.macuyiko.minecraftpyserver;

import java.io.File;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.python.core.PyException;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.InteractiveInterpreter;

public class PyInterpreter extends InteractiveInterpreter {

	private static final AtomicInteger sequence = new AtomicInteger();
	private static final ConcurrentHashMap<Integer,PyInterpreter> interpreters = 
			new ConcurrentHashMap<Integer,PyInterpreter>();
	private final int id;
	private long lastCall;
	
	private static final int IDLE_TIMEOUT = 60 * 15;
	
	public PyInterpreter() {
		super(null, getPythonSystemState());
		this.id = sequence.incrementAndGet();
		interpreters.put(this.id, this);
		this.lastCall = System.currentTimeMillis();
	}
	
	public static void cleanIdle() {
		Iterator<PyInterpreter> it = interpreters.values().iterator();
		while (it.hasNext()) {
			PyInterpreter interpreter = it.next();
			if (interpreter.getSecondsPassedSinceLastCall() >= IDLE_TIMEOUT) {
				interpreter.cleanAndClose();
			}
		}
	}
	
	public boolean isAlive() {
		cleanIdle();
		return interpreters.containsKey(this.id);
	}
	
	public void cleanAndClose() {
		interpreters.remove(this.id);
		this.cleanup();
		this.close();
	}
	
	public double getSecondsPassedSinceLastCall() {
		return (System.currentTimeMillis() - this.lastCall) / 1000D;
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
		PyInterpreter other = (PyInterpreter) obj;
		if (id != other.id) return false;
		return true;
	}

	public boolean parse(String code, boolean exec) {
		lastCall = System.currentTimeMillis();
		try {
			if (exec) this.exec(code);
			else return this.runsource(code);
		} catch (PyException e) {
			throw e;
		}
		return false;
	}

	public boolean parse(File script) {
		lastCall = System.currentTimeMillis();
		try {
			this.execfile(script.getAbsolutePath());
		} catch (PyException e) {
			throw e;
		}
		return false;
	}
	
	public static PySystemState getPythonSystemState() {
		PySystemState sys = new PySystemState();
		addPathToPySystemState(sys, "./");
		addPathToPySystemState(sys, "./python/");
		addPathToPySystemState(sys, "./python-plugins/");
		return sys;
	}
	
	public static void addPathToPySystemState(PySystemState sys, String path) {
		try {
			sys.path.append(new PyString(path));
			File dependencyDirectory = new File(path);
			File[] files = dependencyDirectory.listFiles();
			for (int i = 0; i < files.length; i++) {
			    if (files[i].getName().endsWith(".jar")) {
			    	sys.path.append(new PyString(
			    			new File(path+files[i].getName()).getAbsolutePath()));
			    }
			}
		} catch (Exception e){}
	}
	
}
