package com.erosmari.lumen.commands;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.connections.CoreProtectCompatibility;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.utils.CoreProtectUtils;
import com.erosmari.lumen.utils.RemoveLightUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.remove.only_players")).color(NamedTextColor.RED));
            return 0;
        }

        Location playerLocation = player.getLocation();
        List<Location> blocks = LightRegistry.getBlocksInRange(playerLocation, range);

        CoreProtectCompatibility coreProtectCompatibility = plugin.getCoreProtectCompatibility();

        if (coreProtectCompatibility == null || !coreProtectCompatibility.isEnabled()) {
            player.sendMessage(Component.text(TranslationHandler.get("command.remove.coreprotect_not_available")).color(NamedTextColor.RED));
            logger.warning("CoreProtect is not enabled or available. Could not log block removals.");
        }

        int removedCount = removeAndLogBlocks(blocks, player, coreProtectCompatibility);

        if (removedCount > 0) {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.area.success", removedCount, range)).color(NamedTextColor.GREEN));
            logger.info(String.format("Player %s removed %d light blocks in a %d block radius.", player.getName(), removedCount, range));
        } else {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.area.no_blocks", range)).color(NamedTextColor.RED));
            logger.info(String.format("Player %s attempted to remove blocks in a %d block radius, but no blocks were found.", player.getName(), range));
        }

        return 1;
    }

    private int removeAndLogBlocks(List<Location> blocks, Player player, CoreProtectCompatibility coreProtectCompatibility) {
        int removedCount = 0;

        // Lista para bloques registrados en CoreProtect
        List<Location> registeredBlocks = new ArrayList<>();

        for (Location block : blocks) {
            if (RemoveLightUtils.removeLightBlock(block)) {
                removedCount++;
                registeredBlocks.add(block); // Agrega la ubicación a la lista
            }
        }

        // Registrar en CoreProtect si hay bloques eliminados
        if (!registeredBlocks.isEmpty() && coreProtectCompatibility != null && coreProtectCompatibility.isEnabled()) {
            for (Location location : registeredBlocks) {
                // Forzar el registro como Material.LIGHT
                CoreProtectUtils.logRemoval(logger, coreProtectCompatibility, player.getName(), List.of(location), Material.LIGHT);
            }
        }

        return removedCount;
    }
}