package com.erosmari.lumen.commands;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.connections.CoreProtectHandler;
import com.erosmari.lumen.utils.LoggingUtils;
import com.erosmari.lumen.utils.RemoveLightUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class RemoveCommand {

    private final Lumen plugin;

    public RemoveCommand(Lumen plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("remove")
                .requires(source -> source.getSender().hasPermission("lumen.remove"))
                .then(
                        Commands.literal("area")
                                .then(
                                        Commands.argument("range", IntegerArgumentType.integer(1, 100))
                                                .executes(ctx -> {
                                                    int range = ctx.getArgument("range", Integer.class);
                                                    return handleRemoveAreaCommand(ctx.getSource(), range);
                                                })
                                )
                );
    }

    private int handleRemoveAreaCommand(CommandSourceStack source, int range) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("command.remove.only_players"));
            LoggingUtils.logTranslated("command.remove.only_players_log");
            return 0;
        }

        Location playerLocation = player.getLocation();
        List<Location> blocks = LightRegistry.getBlocksInRange(playerLocation, range);

        CoreProtectHandler coreProtectHandler = plugin.getCoreProtectHandler();

        if (coreProtectHandler == null || !coreProtectHandler.isEnabled()) {
            LoggingUtils.sendAndLog(player,"command.remove.coreprotect_not_available");
        }

        int removedCount = removeAndLogBlocks(blocks, player, coreProtectHandler);

        if (removedCount > 0) {
            LoggingUtils.sendAndLog(player,"command.remove.area.success", removedCount, range);
        } else {
            LoggingUtils.sendAndLog(player,"command.remove.area.no_blocks", range);
        }

        return 1;
    }

    private int removeAndLogBlocks(List<Location> blocks, Player player, CoreProtectHandler coreProtectHandler) {
        int removedCount = 0;

        List<Location> registeredBlocks = new ArrayList<>();

        // Eliminar bloques y acumular los procesados
        for (Location block : blocks) {
            if (RemoveLightUtils.removeLightBlock(block)) {
                removedCount++;
                registeredBlocks.add(block);
            }
        }

        // Registrar los bloques eliminados en lotes
        if (!registeredBlocks.isEmpty() && coreProtectHandler != null && coreProtectHandler.isEnabled()) {
            coreProtectHandler.logRemoval(player.getName(), registeredBlocks, Material.LIGHT);
        }

        return removedCount;
    }
}