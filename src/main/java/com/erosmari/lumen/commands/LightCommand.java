package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.BooleanArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.context.CommandContext;
import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.lights.LightHandler;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
                        .argument(IntegerArgument.of("area_blocks")) // Tamaño del área en bloques
                        .argument(IntegerArgument.of("light_level")) // Nivel de luz deseado
                        .argument(BooleanArgument.optional("include_skylight", false)) // Luz natural opcional
                        .handler(LightCommand::handleLightCommand)
        );
    }

    /**
     * Maneja el comando `/lumen light`.
     *
     * @param context Contexto del comando.
     */
    private static void handleLightCommand(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();

        // Verificar que el remitente sea un jugador
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TranslationHandler.get("command.only_players"));
            return;
        }

        // Obtener argumentos
        int areaBlocks = context.get("area_blocks");
        int lightLevel = context.get("light_level");
        boolean includeSkylight = context.getOrDefault("include_skylight", false);

        // Validar nivel de luz
        if (lightLevel < 0 || lightLevel > 15) {
            player.sendMessage(TranslationHandler.get("command.light.invalid_level"));
            return;
        }

        // Generar un identificador único para la operación
        String operationId = java.util.UUID.randomUUID().toString();

        // Instanciar el manejador de luces y colocar luces
        LightHandler lightHandler = new LightHandler(Lumen.getInstance());
        lightHandler.placeLights(player, areaBlocks, lightLevel, includeSkylight, operationId);

        // Notificar al jugador
        player.sendMessage(TranslationHandler.getFormatted("command.light.success", lightLevel, operationId));
    }
}
