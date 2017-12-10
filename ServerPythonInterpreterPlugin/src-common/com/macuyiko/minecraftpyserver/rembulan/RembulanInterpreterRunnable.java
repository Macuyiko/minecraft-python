package com.macuyiko.minecraftpyserver.rembulan;

import java.io.File;
import org.python.core.PyException;

public class RembulanInterpreterRunnable implements Runnable {
	private final RembulanInterpreter interpreter;
    private final String code;
    private final File file;
    private final TaskResult result;
    
    public RembulanInterpreterRunnable(RembulanInterpreter interpreter, String code) {
        this(interpreter, code, null);
    }
    
    public RembulanInterpreterRunnable(RembulanInterpreter interpreter, File file) {
        this(interpreter, null, file);
    }

    private RembulanInterpreterRunnable(RembulanInterpreter interpreter, String code, File file) {
        this.interpreter = interpreter;
        this.code = code;
        this.file = file;
        this.result = new TaskResult();
    }

    public void run() {
		try {
			if (code != null) result.more = interpreter.parse(code);
			else if (file != null) result.more = interpreter.parse(file);
		} catch (PyException e) {
			result.exception = e;
		} finally {
			synchronized (result) {
				result.done = true;
				result.notifyAll();
			}
		}
	}

    public boolean more() throws PyException {
        synchronized (result) {
            while (!result.done) {
                try {
					result.wait();
				} catch (InterruptedException e) {
					break;
				}
            }
        }
        if (result.exception != null)
            throw result.exception;
        return result.more;
    }
    
    class TaskResult {
        boolean more = false;
        PyException exception;
        boolean done = false;
    }
}