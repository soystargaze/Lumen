package com.erosmari.lumen.commands;

import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.utils.TranslationHandler;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class UndoCommand {

    public UndoCommand() {
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
                        Commands.argument("operation_id", StringArgumentType.string())
                                .executes(ctx -> {
                                    String operationId = ctx.getArgument("operation_id", String.class);
                                    return handleUndoCommand(ctx.getSource(), operationId);
                                })
                )
                .executes(ctx -> handleUndoCommand(ctx.getSource(), "last"));
    }

    /**
     * Maneja el comando `/lumen undo`.
     *
     * @param source      Fuente del comando.
     * @param operationId Identificador de la operación.
     * @return Código de éxito (1 para éxito, 0 para fallo).
     */
    private int handleUndoCommand(CommandSourceStack source, String operationId) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.undo.only_players")).color(NamedTextColor.RED));
            return 0;
        }

        if (operationId.equals("last")) {
            operationId = LightRegistry.getLastOperationId();
            if (operationId == null) {
                player.sendMessage(Component.text(TranslationHandler.get("command.undo.no_previous_operations")).color(NamedTextColor.RED));
                return 0;
            }
        }

        int removedBlocks = removeLightBlocksByOperation(operationId);

        if (removedBlocks > 0) {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.undo.success", removedBlocks, operationId)).color(NamedTextColor.GREEN));
            return 1;
        } else {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.undo.no_blocks", operationId)).color(NamedTextColor.RED));
            return 0;
        }
    }

    /**
     * Elimina bloques de luz asociados a una operación.
     *
     * @param operationId Identificador de la operación.
     * @return Número de bloques eliminados.
     */
    private int removeLightBlocksByOperation(String operationId) {
        List<Location> blocks = LightRegistry.getBlocksByOperationId(operationId);
        if (blocks.isEmpty()) {
            return 0;
        }

        int removedCount = 0;
        for (Location location : blocks) {
            if (removeLightBlock(location)) {
                removedCount++;
            }
        }

        LightRegistry.softDeleteBlocksByOperationId(operationId);
        return removedCount;
    }

    /**
     * Elimina un bloque de luz y lo registra en CoreProtect.
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