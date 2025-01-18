package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import com.erosmari.lumen.database.LightRegistry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class RedoCommand {

    /**
     * Registra el comando `/lumen redo`.
     *
     * @param commandManager El administrador de comandos.
     * @param parentBuilder  El constructor del comando principal.
     */
    public static void register(CommandManager<CommandSender> commandManager, Command.Builder<CommandSender> parentBuilder) {
        commandManager.command(
                parentBuilder.literal("redo")
                        .argument(StringArgument.of("operation_id")) // ID de la operación
                        .permission("lumen.redo")
                        .handler(RedoCommand::handleRedoCommand)
        );
    }

    /**
     * Maneja el comando `/lumen redo`.
     */
    private static void handleRedoCommand(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();

        // Validar que sea un jugador
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo los jugadores pueden usar este comando.");
            return;
        }

        // Obtener el ID de la operación
        String operationId = context.get("operation_id");

        // Recuperar los bloques asociados a la operación
        List<Location> blocks = LightRegistry.getBlocksByOperationId(operationId);

        if (blocks.isEmpty()) {
            player.sendMessage("§eNo se encontraron bloques para la operación: §b" + operationId);
            return;
        }

        int replacedCount = 0;
        for (Location blockLocation : blocks) {
            if (blockLocation.getWorld() != null) {
                blockLocation.getBlock().setType(Material.LIGHT);

                // Ajustar el nivel de luz
                org.bukkit.block.data.Levelled lightData = (org.bukkit.block.data.Levelled) blockLocation.getBlock().getBlockData();
                int lightLevel = LightRegistry.getLightLevel(blockLocation); // Implementa este metodo para recuperar el nivel de luz
                lightData.setLevel(lightLevel);
                blockLocation.getBlock().setBlockData(lightData, true);

                replacedCount++;
            }
        }

        player.sendMessage("§aSe han restaurado " + replacedCount + " bloques de luz para la operación: §b" + operationId);
    }
}