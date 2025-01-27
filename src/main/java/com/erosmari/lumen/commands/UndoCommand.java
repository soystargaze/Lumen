package com.erosmari.lumen.commands;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.connections.CoreProtectHandler;
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
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.undo.only_players")).color(NamedTextColor.RED));
            return 0;
        }

        List<Integer> lastOperations = LightRegistry.getLastOperations(count);

        if (lastOperations.isEmpty()) {
            player.sendMessage(Component.text(TranslationHandler.get("command.undo.no_previous_operations")).color(NamedTextColor.RED));
            return 0;
        }

        int totalRemovedBlocks = removeLightBlocksByOperations(lastOperations, player);

        if (totalRemovedBlocks > 0) {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.undo.success", totalRemovedBlocks, count)).color(NamedTextColor.GREEN));
            return 1;
        } else {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.undo.no_blocks", count)).color(NamedTextColor.RED));
            return 0;
        }
    }

    private int removeLightBlocksByOperations(List<Integer> operationIds, Player player) {
        CoreProtectHandler coreProtectHandler = plugin.getCoreProtectHandler();
        List<Location> allRemovedBlocks = new ArrayList<>();

        for (int operationId : operationIds) {
            List<Location> blocks = LightRegistry.getBlocksByOperationId(operationId);
            if (!blocks.isEmpty()) {
                for (Location location : blocks) {
                    if (removeLightBlock(location)) {
                        allRemovedBlocks.add(location); // Acumula las ubicaciones de los bloques eliminados
                    }
                }
                // Marca la operación como eliminada en el registro
                LightRegistry.softDeleteBlocksByOperationId(operationId);
            }
        }

        // Registrar los bloques eliminados en CoreProtect en un único lote
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