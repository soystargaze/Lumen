package com.erosmari.lumen.lights.integrations;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.utils.LoggingUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RedoFAWEHandler {

    public static void handleRedoWithFAWE(Lumen plugin, Player player, Map<Location, Integer> blocksWithLightLevels, int operationId) {
        if (blocksWithLightLevels == null || blocksWithLightLevels.isEmpty()) {
            LoggingUtils.sendAndLog(player, "command.redo.no_blocks_to_restore", operationId);
            return;
        }

        List<Location> locations = new ArrayList<>(blocksWithLightLevels.keySet());
        int lightLevel = blocksWithLightLevels.values().iterator().next();

        if (lightLevel < 0 || lightLevel > 15) {
            LoggingUtils.sendAndLog(player, "command.redo.invalid_light_level", lightLevel);
            return;
        }

        if (FAWEHandler.isFAWEAvailable()) {
            LoggingUtils.sendAndLog(player, "command.redo.fawe_not_available");
            return;
        }

        var coreProtectHandler = plugin.getCoreProtectHandler();
        if (coreProtectHandler == null || !coreProtectHandler.isEnabled()) {
            LoggingUtils.sendAndLog(player, "command.redo.coreprotect_not_available");
        }

        FAWEHandler.placeLightBlocks(locations, lightLevel, player, coreProtectHandler);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            LightRegistry.restoreSoftDeletedBlocksByOperationId(operationId);
            LoggingUtils.sendAndLog(player, "command.redo.restoration_completed", operationId);
        });
    }
}