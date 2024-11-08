package com.byteprofile;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.dynamic.loading.MultipleParentClassLoader;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.StringMatcher;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.jar.JarFile;

import static net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy.RETRANSFORMATION;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;


public class Premain {

    public static void premain(String agentArgs, Instrumentation inst) throws URISyntaxException, IOException, InterruptedException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        injectBootstrapClasses(inst, classLoader);
    }

    private synchronized static void injectBootstrapClasses(Instrumentation instrumentation, ClassLoader classLoadert) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();

        URL url = classLoader.getResource(Premain.class.getName().replace('.', '/') + ".class");

        String resourcePath = null;
        try {
            resourcePath = url.toURI().getSchemeSpecificPart();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        int protocolSeparatorIndex = resourcePath.indexOf(":");
        int resourceSeparatorIndex = resourcePath.indexOf("!/");

        String agentPath = resourcePath.substring(protocolSeparatorIndex + 1, resourceSeparatorIndex);
        File javaagentFile = new File(agentPath);

        //System.out.println("Loading bootstrap agent jar : " + agentPath + '\n');
        //JarFile currentAgentJar = new JarFile(javaagentFile, false);

        JarFile currentAgentJar = new JarFile(javaagentFile, false);

        String agentPathVertx = "/<path>/vertx-instr.jar";
        File javaagentFileVertx = new File(agentPathVertx);
        JarFile agentJarvertx = new JarFile(javaagentFileVertx, false);


        BytecodrClassLoader classLoader1 = new BytecodrClassLoader(new URL[]{javaagentFileVertx.toURL()}, classLoader);


        try {
            Class t = classLoader1.loadClass("com.byteprofile.InstrumentFactory");
            Constructor con = t.getConstructor(ClassLoader.class, ClassLoader.class, Instrumentation.class);
            Object basicMain = con.newInstance(classLoader1, parent, instrumentation);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static ClassLoader getClassloaderOfClass(Instrumentation instrumentation, String className) {
        try {
            Class[] classes = instrumentation.getAllLoadedClasses();

            System.out.println("Check : " + classes.length);

            for (Class c : classes) {
                try {
                    // starts is okay for some internal classes like org.apache.kafka.Kafka.$Abc
                    if (c.getCanonicalName().startsWith(className)) {
                        ClassLoader cl = c.getClassLoader();
                        System.out.print(String.format("Found the class %s is loaded by %s \n", className, cl));
                        return cl;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
