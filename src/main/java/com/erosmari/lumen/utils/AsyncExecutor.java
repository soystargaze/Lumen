package com.erosmari.lumen.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncExecutor {
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static void shutdown() {
        executor.shutdown();
    }
}