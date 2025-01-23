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
import org.bukkit.entity.Player;

import java.util.List;
import java.util.logging.Logger;

@SuppressWarnings("UnstableApiUsage")
public class RemoveCommand {

    private final CoreProtectCompatibility coreProtectCompatibility;
    private final Logger logger;

    public RemoveCommand(Lumen plugin, CoreProtectCompatibility coreProtectCompatibility) {
        this.coreProtectCompatibility = coreProtectCompatibility;
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

        if (!coreProtectCompatibility.isEnabled()) {
            player.sendMessage(Component.text(TranslationHandler.get("command.remove.coreprotect_not_available")).color(NamedTextColor.RED));
            logger.warning("CoreProtect is not enabled. Could not log block removals.");
            return 0;
        }

        int removedCount = removeLightBlocks(blocks, player);

        if (removedCount > 0) {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.area.success", removedCount, range)).color(NamedTextColor.GREEN));
            logger.info(String.format("Player %s removed %d light blocks in a %d block radius.", player.getName(), removedCount, range));
        } else {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.area.no_blocks", range)).color(NamedTextColor.RED));
            logger.info(String.format("Player %s attempted to remove blocks in a %d block radius, but no blocks were found.", player.getName(), range));
        }

        return 1;
    }

    private int removeLightBlocks(List<Location> blocks, Player player) {
        int removedCount = 0;
        for (Location block : blocks) {
            if (RemoveLightUtils.removeLightBlock(block)) {
                // Registrar la eliminación en CoreProtect
                CoreProtectUtils.logLightRemoval(logger, coreProtectCompatibility, player.getName(), block);
                removedCount++;
            }
        }
        return removedCount;
    }
}