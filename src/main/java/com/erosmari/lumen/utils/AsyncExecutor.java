package com.erosmari.lumen.utils;

import com.erosmari.lumen.config.ConfigHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncExecutor {
    private static ExecutorService executor;

    /**
     * Inicializa el ExecutorService con el tamaño del pool configurado en config.yml.
     * Debe ser llamado explícitamente después de configurar ConfigHandler.
     */
    public static void initialize() {
        if (executor != null && !executor.isShutdown()) {
            throw new IllegalStateException("AsyncExecutor has been initialized.");
        }
        int poolSize = ConfigHandler.getInt("settings.async_thread_pool_size", 4);
        executor = Executors.newFixedThreadPool(poolSize);
    }

    /**
     * Devuelve el ExecutorService inicializado.
     *
     * @return el ExecutorService.
     * @throws IllegalStateException si no se ha inicializado.
     */
    public static ExecutorService getExecutor() {
        if (executor == null) {
            throw new IllegalStateException("AsyncExecutor has not been initialized.");
        }
        return executor;
    }

    /**
     * Cierra el ExecutorService de manera controlada.
     */
    public static void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}