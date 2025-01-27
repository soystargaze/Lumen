package com.erosmari.lumen.utils;

import com.erosmari.lumen.database.LightRegistry;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class BatchProcessor {
    private static final BlockingQueue<BatchEntry> batchQueue = new LinkedBlockingQueue<>();
    private static final int BATCH_SIZE = 1000;
    private static final long BATCH_DELAY_MS = 500;
    private static final Logger logger = Logger.getLogger("Lumen-BatchProcessor");
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    static {
        executor.scheduleAtFixedRate(() -> {
            try {
                processBatch();
            } catch (Exception e) {
                logger.severe("Error while processing batch: " + e.getMessage());
            }
        }, 0, BATCH_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Agrega un bloque al lote.
     *
     * @param location   Ubicación del bloque.
     * @param lightLevel Nivel de luz.
     * @param operationId ID de la operación.
     */
    public static void addBlockToBatch(Location location, int lightLevel, int operationId) {
        boolean success = batchQueue.offer(new BatchEntry(location, lightLevel, operationId));
        if (!success) {
            logger.warning(TranslationHandler.getFormatted("light_registry.error.add_block_failed", location, operationId));

        }
    }

    /**
     * Procesa los bloques acumulados en la cola.
     */
    private static void processBatch() {
        List<BatchEntry> batch = new ArrayList<>();
        batchQueue.drainTo(batch, BATCH_SIZE); // Extrae hasta BATCH_SIZE elementos de la cola

        if (!batch.isEmpty()) {
            int operationId = batch.getFirst().operationId();
            List<Location> locations = new ArrayList<>();
            int lightLevel = batch.getFirst().lightLevel();

            for (BatchEntry entry : batch) {
                locations.add(entry.location());
            }

            LightRegistry.addBlocksAsync(locations, lightLevel, operationId);
            logger.info(TranslationHandler.getFormatted("light_registry.info.batch_processed", locations.size(), operationId));
        }
    }

    /**
     * Clase interna para representar entradas de lotes.
     */
    private record BatchEntry(Location location, int lightLevel, int operationId) {
    }
}