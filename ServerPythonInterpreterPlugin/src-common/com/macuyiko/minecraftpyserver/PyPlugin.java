package com.macuyiko.minecraftpyserver;

import java.io.File;

import org.python.core.PyException;

public interface PyPlugin {
	public void log(String message);
	public boolean parse(PyInterpreter interpreter, String code, boolean exec) throws PyException;
	public boolean parse(PyInterpreter interpreter, final File script) throws PyException;
	public void send(String player, String message);
}
