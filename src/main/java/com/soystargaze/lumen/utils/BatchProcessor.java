package com.soystargaze.lumen.utils;

import com.soystargaze.lumen.database.LightRegistry;
import com.soystargaze.lumen.utils.text.TextHandler;
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
                TextHandler.get().logTranslated("light.error.batch_failed" + e.getMessage());
            }
        }, 0, BATCH_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    public static void addBlockToBatch(Location location, int lightLevel, int operationId) {
        boolean success = batchQueue.offer(new BatchEntry(location, lightLevel, operationId));
        if (!success) {
            TextHandler.get().logTranslated("light_registry.error.add_block_failed", location, operationId);

        }
    }

    private static void processBatch() {
        List<BatchEntry> batch = new ArrayList<>();
        batchQueue.drainTo(batch, BATCH_SIZE);

        if (!batch.isEmpty()) {
            int operationId = batch.getFirst().operationId();
            List<Location> locations = new ArrayList<>();
            int lightLevel = batch.getFirst().lightLevel();

            for (BatchEntry entry : batch) {
                locations.add(entry.location());
            }

            LightRegistry.addBlocksAsync(locations, lightLevel, operationId);
            TextHandler.get().logTranslated("light_registry.info.batch_processed", locations.size(), operationId);
        }
    }

    private record BatchEntry(Location location, int lightLevel, int operationId) {
    }
}