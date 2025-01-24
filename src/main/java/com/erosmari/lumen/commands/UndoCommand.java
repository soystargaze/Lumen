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

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class UndoCommand {

    private final Lumen plugin;

    public UndoCommand(Lumen plugin) {
        this.plugin = plugin;
    }

    /**
     * Registra el subcomando `/lumen undo` en el sistema nativo de Paper.
     *
     * @return Nodo literal del comando para registrarlo en el comando principal.
     */
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

    /**
     * Maneja el comando `/lumen undo`.
     *
     * @param source Fuente del comando.
     * @param count  Número de operaciones a deshacer.
     * @return Código de éxito (1 para éxito, 0 para fallo).
     */
    private int handleUndoCommand(CommandSourceStack source, int count) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.undo.only_players")).color(NamedTextColor.RED));
            return 0;
        }

        List<String> lastOperations = LightRegistry.getLastOperations(count);

        if (lastOperations.isEmpty()) {
            player.sendMessage(Component.text(TranslationHandler.get("command.undo.no_previous_operations")).color(NamedTextColor.RED));
            return 0;
        }

        int totalRemovedBlocks = 0;

        for (String operationId : lastOperations) {
            totalRemovedBlocks += removeLightBlocksByOperation(operationId, player);
        }

        if (totalRemovedBlocks > 0) {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.undo.success", totalRemovedBlocks, count)).color(NamedTextColor.GREEN));
            return 1;
        } else {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.undo.no_blocks", count)).color(NamedTextColor.RED));
            return 0;
        }
    }

    /**
     * Elimina bloques de luz asociados a una operación y los registra en CoreProtect.
     *
     * @param operationId Identificador de la operación.
     * @param player      Jugador que ejecutó el comando.
     * @return Número de bloques eliminados.
     */
    private int removeLightBlocksByOperation(String operationId, Player player) {
        List<Location> blocks = LightRegistry.getBlocksByOperationId(operationId);
        if (blocks.isEmpty()) {
            return 0;
        }

        CoreProtectHandler coreProtectHandler = plugin.getCoreProtectHandler();
        int removedCount = 0;

        for (Location location : blocks) {
            if (removeLightBlock(location)) {
                removedCount++;
                // Registrar cada bloque eliminado en CoreProtect
                if (coreProtectHandler != null && coreProtectHandler.isEnabled()) {
                    coreProtectHandler.logRemoval(player.getName(), List.of(location), Material.LIGHT);
                }
            }
        }

        LightRegistry.softDeleteBlocksByOperationId(operationId);
        return removedCount;
    }

    /**
     * Elimina un bloque de luz.
     *
     * @param location Ubicación del bloque.
     * @return True si el bloque fue eliminado, False en caso contrario.
     */
    private boolean removeLightBlock(Location location) {
        if (location.getBlock().getType() == org.bukkit.Material.LIGHT) {
            // Eliminar el bloque
            location.getBlock().setType(org.bukkit.Material.AIR, false);
            return true;
        }
        return false;
    }
}