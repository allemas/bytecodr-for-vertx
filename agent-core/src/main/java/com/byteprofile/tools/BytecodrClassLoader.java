package com.byteprofile.tools;


import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

public class BytecodrClassLoader extends URLClassLoader {
    private final ClassLoader additionalClassloader;

    public BytecodrClassLoader(URL[] urls, ClassLoader additionalClassloader) {
        super(urls, ClassLoader.getSystemClassLoader().getParent());
        this.additionalClassloader = additionalClassloader;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        if (name != null && (name.startsWith("sun.") || name.startsWith("java."))) {
            return super.loadClass(name, resolve);
        }
        if (additionalClassloader != null) {
            // try load from additional classloader
            try {
                loadedClass = additionalClassloader.loadClass(name);
                return loadedClass;
            } catch (Exception ignore) {
                System.out.println("Not found for " + name + "continue 1");
            }
        }
        try {
            loadedClass = this.getParent().loadClass(name);

            return loadedClass;
        } catch (Exception e) {
            System.out.println("Not found for " + name + "continue 2");

        }
        try {
            Class<?> aClass = findClass(name);
            if (resolve) {
                resolveClass(aClass);
            }
            return aClass;
        } catch (Exception e) {
            System.out.println("Not found for " + name + "STOP");

        }
        return super.loadClass(name, resolve);
    }
}
