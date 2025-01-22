package com.erosmari.lumen.commands;

import com.erosmari.lumen.connections.CoreProtectCompatibility;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.utils.RemoveLightUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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

    public RemoveCommand(CoreProtectCompatibility coreProtectCompatibility, Logger logger) {
        this.coreProtectCompatibility = coreProtectCompatibility;
        this.logger = logger;
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
                )
                .then(
                        Commands.literal("operation")
                                .then(
                                        Commands.argument("operation_id", StringArgumentType.string())
                                                .executes(ctx -> {
                                                    String operationId = ctx.getArgument("operation_id", String.class);
                                                    return handleRemoveOperationCommand(ctx.getSource(), operationId);
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

        int removedCount = removeLightBlocks(player, blocks);

        if (removedCount > 0) {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.area.success", removedCount, range)).color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.area.no_blocks", range)).color(NamedTextColor.RED));
        }

        return 1;
    }

    private int handleRemoveOperationCommand(CommandSourceStack source, String operationId) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.remove.only_players")).color(NamedTextColor.RED));
            return 0;
        }

        List<Location> blocks = LightRegistry.getBlocksByOperationId(operationId);

        if (blocks.isEmpty()) {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.operation.no_blocks", operationId)).color(NamedTextColor.RED));
            return 0;
        }

        int removedCount = removeLightBlocks(player, blocks);

        // Eliminar bloques del registro
        LightRegistry.removeBlocksByOperationId(operationId);

        if (removedCount > 0) {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.operation.success", removedCount, operationId)).color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.operation.no_blocks", operationId)).color(NamedTextColor.RED));
        }

        return 1;
    }

    private int removeLightBlocks(Player player, List<Location> blocks) {
        int removedCount = 0;
        for (Location block : blocks) {
            if (RemoveLightUtils.removeLightBlock(logger, player, block, coreProtectCompatibility)) {
                removedCount++;
            }
        }
        return removedCount;
    }
}