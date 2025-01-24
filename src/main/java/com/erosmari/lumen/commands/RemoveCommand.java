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

        CoreProtectHandler coreProtectHandler = plugin.getCoreProtectHandler();

        if (coreProtectHandler == null || !coreProtectHandler.isEnabled()) {
            player.sendMessage(Component.text(TranslationHandler.get("command.remove.coreprotect_not_available")).color(NamedTextColor.RED));
            logger.warning(TranslationHandler.get("command.remove.coreprotect_disabled_log"));
        }

        int removedCount = removeAndLogBlocks(blocks, player, coreProtectHandler);

        if (removedCount > 0) {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.area.success", removedCount, range)).color(NamedTextColor.GREEN));
            logger.info(TranslationHandler.getFormatted("command.remove.area.success_log", player.getName(), removedCount, range));
        } else {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.area.no_blocks", range)).color(NamedTextColor.RED));
            logger.info(TranslationHandler.getFormatted("command.remove.area.no_blocks_log", player.getName(), range));
        }

        return 1;
    }

    private int removeAndLogBlocks(List<Location> blocks, Player player, CoreProtectHandler coreProtectHandler) {
        int removedCount = 0;

        List<Location> registeredBlocks = new ArrayList<>();

        for (Location block : blocks) {
            if (RemoveLightUtils.removeLightBlock(block)) {
                removedCount++;
                registeredBlocks.add(block);
            }
        }

        if (!registeredBlocks.isEmpty() && coreProtectHandler != null && coreProtectHandler.isEnabled()) {
            for (Location location : registeredBlocks) {
                coreProtectHandler.logRemoval(player.getName(), List.of(location), Material.LIGHT);
            }
        }

        return removedCount;
    }
}