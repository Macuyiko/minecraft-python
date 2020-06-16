package com.macuyiko.minecraftpyserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MinecraftPyServerUtils {

	public static void setup(ClassLoader classLoader) {
		unpack(".", "lib-common/");
		unpack(".", "python/");

		addURLs(classLoader, new File("lib-common/"));
		addURLs(classLoader, new File("lib-custom/"));
	}
	
	public static URL getLocation(final Class<?> c) {
	    if (c == null) return null;
	    try {
	        final URL codeSourceLocation =
	            c.getProtectionDomain().getCodeSource().getLocation();
	        if (codeSourceLocation != null) return codeSourceLocation;
	    }
	    catch (final SecurityException e) {
	    }
	    catch (final NullPointerException e) {
	    }

	    final URL classResource = c.getResource(c.getSimpleName() + ".class");
	    if (classResource == null) return null; // cannot find class resource
	    final String url = classResource.toString();
	    final String suffix = c.getCanonicalName().replace('.', '/') + ".class";
	    if (!url.endsWith(suffix)) return null;
	    final String base = url.substring(0, url.length() - suffix.length());
	    String path = base;
	    if (path.startsWith("jar:")) path = path.substring(4, path.length() - 2);
	    try {
	        return new URL(path);
	    } catch (final MalformedURLException e) {
	        e.printStackTrace();
	        return null;
	    }
	}
	
	public static String os() {
		final String osName = System.getProperty("os.name");
		return osName == null ? "Unknown" : osName;
	}
	
	public static File urlToFile(final String url) {
	    String path = url;
	    if (path.startsWith("jar:")) {
	        final int index = path.indexOf("!/");
	        path = path.substring(4, index);
	    }
	    try {
	        if (os().startsWith("Win") && path.matches("file:[A-Za-z]:.*")) {
	            path = "file:/" + path.substring(5);
	        }
	        return new File(new URL(path).toURI());
	    }
	    catch (final MalformedURLException e) {
	    	
	    }
	    catch (final URISyntaxException e) {
	    	
	    }
	    if (path.startsWith("file:")) {
	        path = path.substring(5);
	        return new File(path);
	    }
	    throw new IllegalArgumentException("[MinecraftPyServer] Invalid URL: " + url);
	}
	
	public static void unpack(String destDir, String prefix) {
		File df = new File(destDir + java.io.File.separator + prefix);
		df.mkdirs();
		
		for (File c : df.listFiles())
			if (c.isFile())
				c.delete();
		
		try (JarFile jar = new JarFile(
				MinecraftPyServerUtils.class.getProtectionDomain()
					.getCodeSource().getLocation().toURI().getPath())) {
			Enumeration<JarEntry> enumEntries = jar.entries();
			while (enumEntries.hasMoreElements()) {
				JarEntry file = enumEntries.nextElement();
				if (!file.getName().startsWith(prefix))
					continue;
				File f = new File(destDir + java.io.File.separator + file.getName());
				System.err.println("[MinecraftPyServer] Unpacking: " + file.getName());
				f.getParentFile().mkdirs();
				if (f.isDirectory())
					continue;
				try (	InputStream in = new BufferedInputStream(jar.getInputStream(file));
						OutputStream out = new BufferedOutputStream(new FileOutputStream(f))) {
					byte[] buffer = new byte[2048];
					while (true) {
						int nBytes = in.read(buffer);
						if (nBytes <= 0)
							break;
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

	public static void addURLs(ClassLoader classLoader, File directory) {
		if (!directory.exists() || !directory.isDirectory())
			return;

		File[] files = directory.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].getName().endsWith(".jar")) {
				try {
					addURL(classLoader, files[i].toURI().toURL());
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void addURL(ClassLoader loader, URL u) {
		System.err.println("[MinecraftPyServer] Adding: " + u.toString());
		try {
			URLClassLoader sysloader = (URLClassLoader) loader;
			Class<URLClassLoader> sysclass = URLClassLoader.class;
			Method method = sysclass.getDeclaredMethod("addURL", new Class[] { URL.class });
			method.setAccessible(true);
			method.invoke(sysloader, new Object[] { u });
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static File matchPythonFile(String arg) {
		String homePath = javax.swing.filechooser.FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
		File asIs = new File(arg);
		File onDesktop = new File(homePath, arg);
		File asIsPy = new File(arg + ".py");
		File onDesktopPy = new File(homePath, arg + ".py");
		if (asIs.exists())
			return asIs;
		if (onDesktop.exists())
			return onDesktop;
		if (asIsPy.exists())
			return asIsPy;
		if (onDesktopPy.exists())
			return onDesktopPy;
		return null;
	}
}
