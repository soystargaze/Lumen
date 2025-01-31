package com.erosmari.lumen.commands;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.connections.CoreProtectHandler;
import com.erosmari.lumen.utils.LoggingUtils;
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
public class UndoCommand {

    private final Lumen plugin;

    public UndoCommand(Lumen plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("undo")
                .requires(source -> source.getSender().hasPermission("lumen.undo"))
                .then(
                        Commands.argument("count", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    int count = ctx.getArgument("count", Integer.class);
                                    return handleUndoCommand(ctx.getSource(), count);
                                })
                )
                .executes(ctx -> handleUndoCommand(ctx.getSource(), 1)); // Por defecto deshace una operación
    }

    private int handleUndoCommand(CommandSourceStack source, int count) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("command.undo.only_players"));
            LoggingUtils.logTranslated("command.undo.only_players");
            return 0;
        }

        List<Integer> lastOperations = LightRegistry.getLastOperations(count);

        if (lastOperations.isEmpty()) {
            LoggingUtils.sendAndLog(player, "command.undo.no_previous_operations");
            return 0;
        }

        int totalRemovedBlocks = removeLightBlocksByOperations(lastOperations, player);

        if (totalRemovedBlocks > 0) {
            LoggingUtils.sendAndLog(player, "command.undo.success", totalRemovedBlocks, count);
            return 1;
        } else {
            LoggingUtils.sendAndLog(player, "command.undo.no_blocks", count);
            return 0;
        }
    }

    private int removeLightBlocksByOperations(List<Integer> operationIds, Player player) {
        CoreProtectHandler coreProtectHandler = plugin.getCoreProtectHandler();
        List<Location> allRemovedBlocks = new ArrayList<>();

        for (int operationId : operationIds) {
            List<Location> blocks = LightRegistry.getBlocksByOperationId(operationId);

            if (!blocks.isEmpty()) {
                List<Location> removedBlocks = new ArrayList<>();
                for (Location location : blocks) {
                    if (removeLightBlock(location)) {
                        removedBlocks.add(location);
                    }
                }

                // Solo marcar la operación como eliminada si se eliminaron bloques
                if (!removedBlocks.isEmpty()) {
                    LightRegistry.softDeleteBlocksByOperationId(operationId);
                    allRemovedBlocks.addAll(removedBlocks);
                }
            }
        }

        // Registrar en CoreProtect solo si hay bloques eliminados
        if (!allRemovedBlocks.isEmpty() && coreProtectHandler != null && coreProtectHandler.isEnabled()) {
            coreProtectHandler.logRemoval(player.getName(), allRemovedBlocks, Material.LIGHT);
        }

        return allRemovedBlocks.size(); // Retorna el número total de bloques eliminados
    }

    private boolean removeLightBlock(Location location) {
        if (location.getBlock().getType() == Material.LIGHT) {
            location.getBlock().setType(Material.AIR, false);
            return true;
        }
        return false;
    }
}