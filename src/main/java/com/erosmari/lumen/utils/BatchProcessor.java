package com.erosmari.lumen.utils;

import com.erosmari.lumen.database.LightRegistry;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BatchProcessor {
    private static final BlockingQueue<BatchEntry> batchQueue = new LinkedBlockingQueue<>();
    private static final int BATCH_SIZE = 1000;
    private static final long BATCH_DELAY_MS = 500;
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    static {
        executor.scheduleAtFixedRate(() -> {
            try {
                processBatch();
            } catch (Exception e) {
                LoggingUtils.logTranslated("light.error.batch_failed" + e.getMessage());
            }
        }, 0, BATCH_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    public static void addBlockToBatch(Location location, int lightLevel, int operationId) {
        boolean success = batchQueue.offer(new BatchEntry(location, lightLevel, operationId));
        if (!success) {
            LoggingUtils.logTranslated("light_registry.error.add_block_failed", location, operationId);

        }
    }

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
            LoggingUtils.logTranslated("light_registry.info.batch_processed", locations.size(), operationId);
        }
    }

    private record BatchEntry(Location location, int lightLevel, int operationId) {
    }
}