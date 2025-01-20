package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class RemoveCommand {

    /**
     * Registra el comando `/lumen remove`.
     *
     * @param commandManager El administrador de comandos.
     * @param parentBuilder  El constructor del comando principal.
     */
    public static void register(CommandManager<CommandSender> commandManager, Command.Builder<CommandSender> parentBuilder) {
        commandManager.command(
                parentBuilder.literal("remove")
                        .permission("lumen.remove")
                        .literal("area")
                        .argument(IntegerArgument.of("range")) // Rango del área
                        .handler(RemoveCommand::handleRemoveAreaCommand)
        );

        commandManager.command(
                parentBuilder.literal("remove")
                        .permission("lumen.remove")
                        .literal("operation")
                        .argument(StringArgument.of("operation_id")) // Identificador de operación
                        .handler(RemoveCommand::handleRemoveOperationCommand)
        );
    }

    /**
     * Maneja el comando `/lumen remove area <range>`.
     */
    private static void handleRemoveAreaCommand(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();

        // Validar que sea un jugador
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TranslationHandler.get("command.remove.only_players"));
            return;
        }

        // Obtener el rango del comando
        int range = context.get("range");
        Location playerLocation = player.getLocation();

        // Obtener y eliminar bloques dentro del rango
        List<Location> blocks = LightRegistry.getBlocksInRange(playerLocation, range);

        int removedCount = 0;
        for (Location block : blocks) {
            if (block.getBlock().getType() == Material.LIGHT) {
                block.getBlock().setType(Material.AIR);
                removedCount++;
            }
        }

        // Informar al jugador
        if (removedCount > 0) {
            player.sendMessage(TranslationHandler.getFormatted("command.remove.area.success", removedCount, range));
        } else {
            player.sendMessage(TranslationHandler.getFormatted("command.remove.area.no_blocks", range));
        }
    }

    /**
     * Maneja el comando `/lumen remove operation <operation_id>`.
     */
    private static void handleRemoveOperationCommand(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();

        // Validar que sea un jugador
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TranslationHandler.get("command.remove.only_players"));
            return;
        }

        // Obtener el ID de la operación
        String operationId = context.get("operation_id");

        // Recuperar los bloques asociados a la operación
        List<Location> blocks = LightRegistry.getBlocksByOperationId(operationId);

        if (blocks.isEmpty()) {
            player.sendMessage(TranslationHandler.getFormatted("command.remove.operation.no_blocks", operationId));
            return;
        }

        int removedCount = 0;
        for (Location blockLocation : blocks) {
            if (blockLocation.getWorld() != null && blockLocation.getBlock().getType() == Material.LIGHT) {
                blockLocation.getBlock().setType(Material.AIR);
                removedCount++;
            }
        }

        // Eliminar los registros de la base de datos
        LightRegistry.removeBlocksByOperationId(operationId);

        // Informar al jugador
        if (removedCount > 0) {
            player.sendMessage(TranslationHandler.getFormatted("command.remove.operation.success", removedCount, operationId));
        } else {
            player.sendMessage(TranslationHandler.getFormatted("command.remove.operation.no_blocks", operationId));
        }
    }
}
