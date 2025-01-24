package com.erosmari.lumen.lights.integrations;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RedoFAWEHandler {

    public static void handleRedoWithFAWE(Lumen plugin, Player player, Map<Location, Integer> blocksWithLightLevels, String operationId) {
        List<Location> locations = new ArrayList<>(blocksWithLightLevels.keySet());
        int lightLevel = blocksWithLightLevels.values().iterator().next();

        // Usar FAWEHandler para colocar bloques
        FAWEHandler.placeLightBlocks(
                locations,
                lightLevel,
                player,
                plugin,
                plugin.getCoreProtectHandler()
        );

        // Actualizar el registro de LightRegistry después de completar la colocación
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getLogger().info(TranslationHandler.getFormatted("command.redo.restoration_completed_log", operationId));
            LightRegistry.restoreSoftDeletedBlocksByOperationId(operationId);
            player.sendMessage(TranslationHandler.getFormatted("command.redo.restoration_completed", operationId));
        });
    }
}