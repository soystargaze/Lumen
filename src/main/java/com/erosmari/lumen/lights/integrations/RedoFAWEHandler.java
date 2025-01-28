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
        List<Location> locations = new ArrayList<>(blocksWithLightLevels.keySet());
        int lightLevel = blocksWithLightLevels.values().iterator().next();

        // Usar FAWEHandler para colocar bloques
        FAWEHandler.placeLightBlocks(
                locations,
                lightLevel,
                player,
                plugin.getCoreProtectHandler()
        );

        // Actualizar el registro de LightRegistry después de completar la colocación
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            LightRegistry.restoreSoftDeletedBlocksByOperationId(operationId);
            LoggingUtils.sendAndLog(player,"command.redo.restoration_completed", operationId);
        });
    }
}