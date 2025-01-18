package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.context.CommandContext;
import com.erosmari.lumen.database.LightRegistry;
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
                        .argument(IntegerArgument.of("range")) // Rango del área
                        .permission("lumen.remove")
                        .handler(RemoveCommand::handleRemoveCommand)
        );
    }

    /**
     * Maneja el comando `/lumen remove`.
     */
    private static void handleRemoveCommand(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();

        // Validar que sea un jugador
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo los jugadores pueden usar este comando.");
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
            player.sendMessage("§aSe han eliminado " + removedCount + " bloques de luz en un rango de " + range + " bloques.");
        } else {
            player.sendMessage("§eNo se encontraron bloques de luz en el rango especificado.");
        }
    }
}