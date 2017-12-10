package com.macuyiko.minecraftpyserver.rembulan;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.sandius.rembulan.StateContext;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.Variable;
import net.sandius.rembulan.compiler.CompilerChunkLoader;
import net.sandius.rembulan.compiler.CompilerSettings;
import net.sandius.rembulan.env.RuntimeEnvironment;
import net.sandius.rembulan.env.RuntimeEnvironments;
import net.sandius.rembulan.exec.CallException;
import net.sandius.rembulan.exec.CallPausedException;
import net.sandius.rembulan.exec.DirectCallExecutor;
import net.sandius.rembulan.impl.StateContexts;
import net.sandius.rembulan.lib.StandardLibrary;
import net.sandius.rembulan.load.LoaderException;
import net.sandius.rembulan.runtime.LuaFunction;

public class RembulanInterpreter {

	private static final AtomicInteger sequence = new AtomicInteger();
	private static final ConcurrentHashMap<Integer,RembulanInterpreter> interpreters = 
			new ConcurrentHashMap<Integer,RembulanInterpreter>();
	private final int id;
	private long lastCall;
	private StateContext state;
	private StringBuilder codeBuffer;
	private CompilerChunkLoader loader;
	private DirectCallExecutor callExecutor;
	private Table env;
	private boolean more;
	private LuaFunction printFunction;
	private PrintStream out;
	private PrintStream err;
	
	private static final String SOURCE_STDIN = "stdin";
	private static final int IDLE_TIMEOUT = 60 * 15;
	
	public RembulanInterpreter() {
		this(System.out, System.err);
	}
		
	public RembulanInterpreter(PrintStream out, PrintStream err) {
		this.id = sequence.incrementAndGet();
		interpreters.put(this.id, this);
		this.lastCall = System.currentTimeMillis();
		this.state = StateContexts.newDefaultInstance();
		this.codeBuffer = new StringBuilder();
		this.out = out;
		this.err = err;
		CompilerSettings compilerSettings = CompilerSettings.defaultSettings();
		RuntimeEnvironment runtimeEnv = RuntimeEnvironments.system(System.in, this.out, this.err);
		this.loader = CompilerChunkLoader.of(compilerSettings, "rembulan_repl_");
		URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		this.env = StandardLibrary.in(runtimeEnv)
				.withLoader(loader)
				.withModuleLoader(sysloader)
				.withDebug(true)
				.installInto(state);
		this.more = false;
		this.callExecutor = DirectCallExecutor.newExecutor();
		printFunction = Auxf.callGlobal(env, "print");
	}

	public static void cleanIdle() {
		Iterator<RembulanInterpreter> it = interpreters.values().iterator();
		while (it.hasNext()) {
			RembulanInterpreter interpreter = it.next();
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
		RembulanInterpreter other = (RembulanInterpreter) obj;
		if (id != other.id) return false;
		return true;
	}

	public boolean parse(String code) {
		lastCall = System.currentTimeMillis();
		LuaFunction fn = null;
		
		boolean firstLine = codeBuffer.length() == 0;
		boolean emptyInput = code.trim().isEmpty();
		
		if (firstLine && !emptyInput) {
			try {
				fn = loader.loadTextChunk(new Variable(env), SOURCE_STDIN, "return " + code);
			} catch (LoaderException ex) {}
		}

		if (fn == null) {
			codeBuffer.append(code).append('\n');
			try {
				fn = loader.loadTextChunk(new Variable(env), SOURCE_STDIN, codeBuffer.toString());
			} catch (LoaderException ex) {
				if (ex.isPartialInputError()) {
					more = true;
				} else {
					ex.printStackTrace(err);
					codeBuffer.setLength(0);
					more = false;
				}
			}
		}

		if (fn != null) {
			codeBuffer.setLength(0);

			Object[] results = null;
			try {
				results = callFunction(fn);
			} catch (CallException ex) {
				ex.printStackTrace(err);
			}

			if (results != null && results.length > 0) {
				try {
					callFunction(printFunction, results);
				} catch (CallException ex) {
					err.println("error calling 'print'");
				}
			}

			more = false;
		}
		
		return more;
	}

	public boolean parse(File script) {
		lastCall = System.currentTimeMillis();
		try {
			String sourceText = readFile(script);
			LuaFunction fn = loader.loadTextChunk(new Variable(env), script.getName(), sourceText);
			callFunction(fn);
		} catch (CallException | LoaderException | IOException e) {
			e.printStackTrace(err);
		}
		return false;
	}
	
	private Object[] callFunction(LuaFunction fn, Object... args) throws CallException {
		try {
			return callExecutor.call(state, fn, args);
		} catch (CallPausedException | InterruptedException ex) {
			throw new CallException(ex);
		}
	}
	
	public static String bytesToString(byte[] bytes) {
		Charset charset = Charset.forName("ISO-8859-1");
		return new String(bytes, charset);
	}

	public static String readFile(File script) throws IOException {
		return bytesToString(Files.readAllBytes(script.toPath()));
	}

	
}
