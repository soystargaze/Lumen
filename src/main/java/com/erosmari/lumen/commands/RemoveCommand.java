package com.erosmari.lumen.commands;

import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.utils.TranslationHandler;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
public class RemoveCommand {

    /**
     * Registra el subcomando `/lumen remove` en el sistema nativo de Paper.
     *
     * @return Nodo literal del comando para registrarlo en el comando principal.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("remove")
                .requires(source -> source.getSender().hasPermission("lumen.remove"))
                .then(
                        Commands.literal("area")
                                .then(
                                        Commands.argument("range", IntegerArgumentType.integer(1, 100)) // Rango del área
                                                .executes(ctx -> {
                                                    int range = ctx.getArgument("range", Integer.class);
                                                    return handleRemoveAreaCommand(ctx.getSource(), range);
                                                })
                                )
                )
                .then(
                        Commands.literal("operation")
                                .then(
                                        Commands.argument("operation_id", StringArgumentType.string()) // Identificador de operación
                                                .executes(ctx -> {
                                                    String operationId = ctx.getArgument("operation_id", String.class);
                                                    return handleRemoveOperationCommand(ctx.getSource(), operationId);
                                                })
                                )
                );
    }

    /**
     * Maneja el comando `/lumen remove area <range>`.
     *
     * @param source Fuente del comando.
     * @param range  Rango del área en bloques.
     * @return Código de éxito.
     */
    private static int handleRemoveAreaCommand(CommandSourceStack source, int range) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.remove.only_players")).color(NamedTextColor.RED));
            return 0;
        }

        Location playerLocation = player.getLocation();
        List<Location> blocks = LightRegistry.getBlocksInRange(playerLocation, range);

        int removedCount = 0;
        for (Location block : blocks) {
            if (block.getBlock().getType() == Material.LIGHT) {
                block.getBlock().setType(Material.AIR);
                removedCount++;
            }
        }

        if (removedCount > 0) {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.area.success", removedCount, range)).color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.area.no_blocks", range)).color(NamedTextColor.RED));
        }

        return 1;
    }

    /**
     * Maneja el comando `/lumen remove operation <operation_id>`.
     *
     * @param source      Fuente del comando.
     * @param operationId Identificador de la operación.
     * @return Código de éxito.
     */
    private static int handleRemoveOperationCommand(CommandSourceStack source, String operationId) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.remove.only_players")).color(NamedTextColor.RED));
            return 0;
        }

        List<Location> blocks = LightRegistry.getBlocksByOperationId(operationId);

        if (blocks.isEmpty()) {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.operation.no_blocks", operationId)).color(NamedTextColor.RED));
            return 0;
        }

        int removedCount = 0;
        for (Location blockLocation : blocks) {
            if (blockLocation.getWorld() != null && blockLocation.getBlock().getType() == Material.LIGHT) {
                blockLocation.getBlock().setType(Material.AIR);
                removedCount++;
            }
        }

        LightRegistry.removeBlocksByOperationId(operationId);

        if (removedCount > 0) {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.operation.success", removedCount, operationId)).color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.remove.operation.no_blocks", operationId)).color(NamedTextColor.RED));
        }

        return 1;
    }
}