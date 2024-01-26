package com.macuyiko.minecraftpyserver.jython;

import java.io.File;
import java.net.URLClassLoader;
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

import com.macuyiko.minecraftpyserver.MinecraftPyServerUtils;

public class JyInterpreter extends InteractiveInterpreter {

	private static final AtomicInteger sequence = new AtomicInteger();
	private static final ConcurrentHashMap<Integer, JyInterpreter> interpreters = new ConcurrentHashMap<Integer, JyInterpreter>();

	private final int id;
	private long lastCall;
	private boolean permanent;
	private int timeout;
	private List<String> buffer = new ArrayList<String>();

	private static final int DEFAULT_IDLE_TIMEOUT = 60 * 15;

	public static PySystemState getPythonSystemState(ClassLoader parentLoader) {
		PySystemState sys = new PySystemState();

		URLClassLoader load = MinecraftPyServerUtils.createJythonClassLoader(parentLoader);
		sys.setClassLoader(load);

		addPathToPySystemState(sys, "./python/");
		addPathToPySystemState(sys, "./python-plugins/");

		return sys;
	}

	public static void addPathToPySystemState(PySystemState sys, String path) {
		try {
			sys.path.append(new PyString(path));
		} catch (Exception e) {
		}
	}

	public JyInterpreter(ClassLoader parentLoader) {
		this(parentLoader, false, DEFAULT_IDLE_TIMEOUT);
	}

	public JyInterpreter(ClassLoader parentLoader, boolean permanent) {
		this(parentLoader, permanent, DEFAULT_IDLE_TIMEOUT);
	}

	public JyInterpreter(ClassLoader parentLoader, int timeout) {
		this(parentLoader, false, timeout);
	}

	public JyInterpreter(ClassLoader parentLoader, boolean permanent, int timeout) {
		super(null, getPythonSystemState(parentLoader));
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
			if (!interpreter.isPermanent() && interpreter.getTimeout() > 0
					&& interpreter.getSecondsPassedSinceLastCall() >= interpreter.getTimeout()) {
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
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JyInterpreter other = (JyInterpreter) obj;
		if (id != other.id)
			return false;
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
				// We don't want clients to stop the whole JVM, 
				// We do stop this interpreter, however
				this.close();
				return;
			}
			showexception(exc);
		}
	}

}
