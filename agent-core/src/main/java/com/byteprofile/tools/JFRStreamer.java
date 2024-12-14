package com.byteprofile.tools;

import com.byteprofile.Premain;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingStream;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.jar.JarFile;

public class JFRStreamer {
    public static void stream(int period) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(task(), 0, period, TimeUnit.SECONDS);
    }

    private record JFR(String name, Duration period) {
    }

    private static Runnable task() {
        JFR[] listenedJFR = new JFR[]{
                new JFR("jdk.CPULoad", Duration.ofSeconds(2)),
                new JFR("jdk.GCHeapSummary", Duration.ofSeconds(2)),
                new JFR("jdk.GCHeapMemoryUsage", Duration.ofSeconds(2))
        };

        return (() -> {
            try (var rs = new RecordingStream()) {
                Arrays.stream(listenedJFR)
                        .forEach(jfr -> {
                            rs.enable(jfr.name).withPeriod(jfr.period);
                            rs.onEvent(jfr.name, System.out::println);
                        });

                rs.onEvent("jdk.GCHeapMemoryUsage", recordedEvent -> {
                    final Meter meter = GlobalOpenTelemetry.getMeter("GCHeapMemoryUsage");
                    meter.gaugeBuilder("gc_heap_used")
                            .setDescription("Charge totale de la machine")
                            .setUnit("MB")
                            .build()
                            .set(recordedEvent.getDouble("used"));
                });
                rs.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void injectBootstrapClasses(Instrumentation instrumentation, ClassLoader classLoadert) throws IOException {
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
        instrumentation.appendToBootstrapClassLoaderSearch(currentAgentJar);

        String agentPathVertx = "/Users/sebastienallemand/Documents/bytecodr-for-vertx/vert-x-instrumentation/target/vertx-instr-shaded.jar";
        File javaagentFileVertx = new File(agentPathVertx);
        JarFile agentJarvertx = new JarFile(javaagentFileVertx, false);
        instrumentation.appendToBootstrapClassLoaderSearch(agentJarvertx);

        String agentPathVertx2 = "/Users/sebastienallemand/Documents/bytecodr-for-vertx/vert-x-instrumentation/target/vert-x-instrumentation-1.0-SNAPSHOT.jar";
        File javaagentFileVertx2 = new File(agentPathVertx2);
        JarFile agentJarvertx2 = new JarFile(javaagentFileVertx2, false);
        //    instrumentation.appendToBootstrapClassLoaderSearch(agentJarvertx2);

        String agentPathbbuddy = "/Users/sebastienallemand/Documents/bytecodr-for-vertx/bootstrap-bytebuddy/target/bytebuddy-shaded.jar";
        File javaagentFilebbudy = new File(agentPathbbuddy);
        JarFile javaagentjar = new JarFile(javaagentFilebbudy, false);
        instrumentation.appendToBootstrapClassLoaderSearch(javaagentjar);


        BytecodrClassLoader classLoader1 = new BytecodrClassLoader(new URL[]{
                javaagentFileVertx.toURL(),
                javaagentFileVertx2.toURL(),
                javaagentFilebbudy.toURL(),
        }, parent);


        try {
            Class t = classLoader1.loadClass("com.byteprofile.InstrumentFactory");
            Constructor con = t.getConstructor(ClassLoader.class, ClassLoader.class, Instrumentation.class);
            Object basicMain = con.newInstance(classLoader1, parent, instrumentation);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private static void handleCPULoadEvent(RecordedEvent event) {
        float machineTotal = event.getFloat("machineTotal");
        float jvmUser = event.getFloat("jvmUser");
        float jvmSystem = event.getFloat("jvmSystem");
        final Meter meter = GlobalOpenTelemetry.getMeter("CPULoadExample");

        meter.gaugeBuilder("cpu_load_machine_total")
                .setDescription("Charge totale de la machine")
                .setUnit("percent")
                .build()
                .set(Float.valueOf(machineTotal * 100).doubleValue());

        System.out.printf(
                "Charge CPU : Machine=%.2f%%, JVM (User)=%.2f%%, JVM (System)=%.2f%%%n",
                machineTotal * 100, jvmUser * 100, jvmSystem * 100
        );
    }


}
