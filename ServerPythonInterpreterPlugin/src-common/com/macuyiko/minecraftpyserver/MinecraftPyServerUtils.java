package com.macuyiko.minecraftpyserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.macuyiko.minecraftpyserver.py4j.PyClassLoaderHack;

public class MinecraftPyServerUtils {
	public static void setup() throws IOException {
		unpack(".", "lib-common/");
		unpack(".", "python/");
		
		File dependencyDirectory = new File("lib-common/");
		if (!dependencyDirectory.exists() || !dependencyDirectory.isDirectory())
	        return;
	    File[] files = dependencyDirectory.listFiles();
		for (int i = 0; i < files.length; i++) {
		    if (files[i].getName().endsWith(".jar")) {
		    	addURL(new File(dependencyDirectory.getName() + "/" + files[i].getName()).toURI().toURL());
		    }
		}
		
		PyClassLoaderHack.setPy4JClassLoader();
	}
	
	
	
	public static void unpack(String destDir, String prefix) {
		File df = new File(destDir + java.io.File.separator + prefix);
		df.mkdirs();
		for (File c : df.listFiles())
			if (c.isFile()) c.delete();
		try(JarFile jar = new JarFile(MinecraftPyServerUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath())){
			Enumeration<JarEntry> enumEntries = jar.entries();
			while (enumEntries.hasMoreElements()) {
			    JarEntry file = enumEntries.nextElement();
			    if (!file.getName().startsWith(prefix)) continue;
			    File f = new File(destDir + java.io.File.separator + file.getName());
			    System.err.println("[MinecraftPyServer] Unpacking: " + file.getName());
			    if (file.isDirectory()) {
			    	System.err.println("[MinecraftPyServer]           ... creating dir");
				    f.mkdirs();
			        continue;
			    }
			    try(	InputStream in = new BufferedInputStream(jar.getInputStream(file));
			    		OutputStream out = new BufferedOutputStream(new FileOutputStream(f))){
	                byte[] buffer = new byte[2048];
	                while (true) {
	                    int nBytes = in.read(buffer);
	                    if (nBytes <= 0) break;
	                    out.write(buffer, 0, nBytes);
	                }
	                out.flush();
			    } catch (Exception e) {
					e.printStackTrace();
				}
			}
			jar.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	public static void addURL(URL u) throws IOException {
		System.err.println("[MinecraftPyServer] Adding: "+u.toString());
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
	
	public static File matchPythonFile(String arg) {
		File asIs = new File(arg);
		File onDesktop = new File(javax.swing.filechooser.FileSystemView.getFileSystemView()
				.getHomeDirectory().getAbsolutePath(), arg);
		File asIsPy = new File(arg + ".py");
		File onDesktopPy = new File(javax.swing.filechooser.FileSystemView.getFileSystemView()
				.getHomeDirectory().getAbsolutePath(), arg + ".py");
		if (asIs.exists()) return asIs;
		if (onDesktop.exists()) return onDesktop;
		if (asIsPy.exists()) return asIsPy;
		if (onDesktopPy.exists()) return onDesktopPy;
		return null;
	}
}
