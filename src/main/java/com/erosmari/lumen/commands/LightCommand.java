package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.BooleanArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.context.CommandContext;
import com.erosmari.lumen.lights.LightHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LightCommand {

    /**
     * Registra el comando `/lumen light`.
     *
     * @param commandManager El administrador de comandos.
     * @param parentBuilder  El constructor del comando principal.
     */
    public static void register(CommandManager<CommandSender> commandManager, Command.Builder<CommandSender> parentBuilder) {
        commandManager.command(
                parentBuilder.literal("light")
                        .argument(IntegerArgument.of("area_blocks")) // Rango del área
                        .argument(IntegerArgument.of("light_level")) // Nivel de luz
                        .argument(BooleanArgument.of("include_skylight")) // Skylight
                        .handler(LightCommand::handleLightCommand)
        );
    }

    private static void handleLightCommand(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();

        // Validar que sea un jugador
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo los jugadores pueden usar este comando.");
            return;
        }

        // Obtener argumentos
        int areaBlocks = context.get("area_blocks");
        int lightLevel = context.get("light_level");
        boolean includeSkylight = context.get("include_skylight");

        // Validar nivel de luz
        if (lightLevel < 0 || lightLevel > 15) {
            sender.sendMessage("§cEl nivel de luz debe estar entre 0 y 15.");
            return;
        }

        // Generar un identificador único para la operación
        String operationId = UUID.randomUUID().toString();

        // Colocar luces
        LightHandler lightHandler = new LightHandler();
        lightHandler.placeLights(player, areaBlocks, lightLevel, includeSkylight, operationId);

        sender.sendMessage("§aLuces colocadas con nivel de luz " + lightLevel + " en un área de " + areaBlocks + " bloques.");
        sender.sendMessage("§eID de operación: §b" + operationId);
    }
}