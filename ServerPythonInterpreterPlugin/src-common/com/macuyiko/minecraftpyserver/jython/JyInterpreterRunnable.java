package com.macuyiko.minecraftpyserver.jython;

import java.io.File;
import org.python.core.PyException;

public class JyInterpreterRunnable implements Runnable {
	private final JyInterpreter interpreter;
    private final boolean exec;
    private final String code;
    private final File file;
    private final TaskResult result;
    
    public JyInterpreterRunnable(JyInterpreter interpreter, String code, boolean exec) {
        this(interpreter, code, exec, null);
    }
    
    public JyInterpreterRunnable(JyInterpreter interpreter, File file) {
        this(interpreter, null, true, file);
    }

    private JyInterpreterRunnable(JyInterpreter interpreter, String code, boolean exec, File file) {
        this.exec = exec;
        this.interpreter = interpreter;
        this.code = code;
        this.file = file;
        this.result = new TaskResult();
    }

    public void run() {
		try {
			if (code != null) result.more = interpreter.parse(code, exec);
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