package com.macuyiko.minecraftpyserver;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class SetupUtils {
	public static void setup() throws IOException {
		File dependencyDirectory = new File("lib-common/");
		File[] files = dependencyDirectory.listFiles();
		for (int i = 0; i < files.length; i++) {
		    if (files[i].getName().endsWith(".jar")) {
		    	addURL(new File("lib-common/"+files[i].getName()).toURI().toURL());
		    }
		}
	}
	
	public static void addURL(URL u) throws IOException {
    	URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", new Class[] {URL.class});
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] {u});
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }
    }
	
	
}
