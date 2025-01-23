package com.erosmari.lumen.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncExecutor {
    private static final ExecutorService executor = Executors.newFixedThreadPool(4); // Configura el número de hilos

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static void shutdown() {
        executor.shutdown();
    }
}