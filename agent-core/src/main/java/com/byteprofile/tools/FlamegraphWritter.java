package com.byteprofile.tools;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FlamegraphWritter {
    public static void write(int period) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(task(), 0, period, TimeUnit.SECONDS);
    }

    private static Runnable task() {
        return () -> {
            Store store = new Store("/tmp/flamegraph-marsjug.html");
            Thread.getAllStackTraces().forEach((k, stackTraceElements) -> {
                store.addSample(stackTraceElements);
            });
            store.createFlameGraphFile();
        };
    }
}
