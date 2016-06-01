package com.macuyiko.minecraftpyserver;

import java.io.File;

import com.macuyiko.minecraftpyserver.servers.PyWebSocketServer;
import com.macuyiko.minecraftpyserver.servers.PyTelnetServer;

public class ConsolePlugin {
	private ConsolePlugin() {}
		
	public static void start(PyPlugin mainPlugin, int telnetport, int websocketport, String serverpass) {
		if (telnetport > -1) {
			PyTelnetServer server = new PyTelnetServer(mainPlugin, telnetport, serverpass);
			Thread t = new Thread(server);
			t.start();
		}
		
		if (websocketport > -1) {
			PyWebSocketServer webserver = new PyWebSocketServer(mainPlugin, websocketport, serverpass);
			webserver.start();
		}
		
		loadPythonPlugins(mainPlugin, "./python-plugins");
	}

	
	private static void loadPythonPlugins(final PyPlugin mainPlugin, String path) {
        File pluginsDir = new File(path);
        if (!pluginsDir.exists() || !pluginsDir.isDirectory())
            return;
        final File[] files = pluginsDir.listFiles();
        for (final File file : files) {
            if (!file.getName().endsWith(".py")) continue;
            Thread pyPlugin = new Thread() {
                @Override
                public void run() {
                    PyInterpreter interpreter = new PyInterpreter();
                    try {
						mainPlugin.parse(interpreter, file);
					} catch (Throwable e) {
						e.printStackTrace();
					}
                }
            };
            pyPlugin.start();
        }
    }
}
