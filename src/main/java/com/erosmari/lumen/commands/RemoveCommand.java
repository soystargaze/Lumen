package com.erosmari.lumen.commands;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.connections.CoreProtectHandler;
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
import java.util.logging.Logger;

@SuppressWarnings("UnstableApiUsage")
public class RemoveCommand {

    private final Lumen plugin;
    private final Logger logger;

    public RemoveCommand(Lumen plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
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
            return 0;
        }

        Location playerLocation = player.getLocation();
        List<Location> blocks = LightRegistry.getBlocksInRange(playerLocation, range);

        CoreProtectHandler coreProtectHandler = plugin.getCoreProtectHandler();

        if (coreProtectHandler == null || !coreProtectHandler.isEnabled()) {
            player.sendMessage(TranslationHandler.getPlayerMessage("command.remove.coreprotect_not_available"));
            logger.warning(TranslationHandler.get("command.remove.coreprotect_disabled_log"));
        }

        int removedCount = removeAndLogBlocks(blocks, player, coreProtectHandler);

        if (removedCount > 0) {
            player.sendMessage(TranslationHandler.getPlayerMessage("command.remove.area.success", removedCount, range));
            logger.info(TranslationHandler.getFormatted("command.remove.area.success_log", player.getName(), removedCount, range));
        } else {
            player.sendMessage(TranslationHandler.getPlayerMessage("command.remove.area.no_blocks", range));
            logger.info(TranslationHandler.getFormatted("command.remove.area.no_blocks_log", player.getName(), range));
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