package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.BooleanArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.context.CommandContext;
import com.erosmari.lumen.lights.LightHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class LightCommand {

    /**
     * Registra el comando `/lumen light`.
     *
     * @param commandManager El administrador de comandos.
     * @param parentBuilder  El constructor del comando principal.
     * @param plugin         El plugin principal.
     */
    public static void register(CommandManager<CommandSender> commandManager, Command.Builder<CommandSender> parentBuilder, JavaPlugin plugin) {
        commandManager.command(
                parentBuilder.literal("light")
                        .argument(IntegerArgument.of("area_blocks")) // Rango del área
                        .argument(IntegerArgument.of("light_level")) // Nivel de luz
                        .argument(BooleanArgument.of("include_skylight")) // Skylight
                        .handler(context -> handleLightCommand(context, plugin))
        );
    }

    /**
     * Maneja el comando `/lumen light`.
     */
    private static void handleLightCommand(CommandContext<CommandSender> context, JavaPlugin plugin) {
        CommandSender sender = context.getSender();

        // Validar que sea un jugador
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo los jugadores pueden usar este comando.");
            return;
        }

        // Obtener argumentos
        int areaBlocks = context.get("area_blocks");
        int lightLevel = context.get("light_level");
        boolean includeSkylight = context.get("include_skylight");

        // Validar nivel de luz
        if (lightLevel < 0 || lightLevel > 15) {
            sender.sendMessage("El nivel de luz debe estar entre 0 y 15.");
            return;
        }

        // Colocar luces
        LightHandler lightHandler = new LightHandler();
        lightHandler.placeLights(player, areaBlocks, lightLevel, includeSkylight);

        sender.sendMessage("Luces colocadas con nivel de luz " + lightLevel + " en un área de " + areaBlocks + " bloques.");
    }
}
