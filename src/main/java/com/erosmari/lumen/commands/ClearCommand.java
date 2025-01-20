package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClearCommand {

    private static final Map<UUID, Long> confirmationRequests = new HashMap<>(); // Mapa para confirmaciones pendientes
    private static final long CONFIRMATION_TIMEOUT = 30_000; // Tiempo límite para confirmar (30 segundos)

    /**
     * Registra el comando `/lumen clear` y su subcomando `confirm`.
     *
     * @param commandManager El administrador de comandos.
     * @param parentBuilder  El constructor del comando principal.
     */
    public static void register(CommandManager<CommandSender> commandManager, Command.Builder<CommandSender> parentBuilder) {
        // Comando `/lumen clear`
        commandManager.command(
                parentBuilder.literal("clear")
                        .permission("lumen.clear")
                        .handler(ClearCommand::handleClearRequest) // Solicita confirmación
        );

        // Subcomando `/lumen clear confirm`
        commandManager.command(
                parentBuilder.literal("clear")
                        .literal("confirm")
                        .permission("lumen.clear")
                        .handler(ClearCommand::handleClearConfirm) // Ejecuta la limpieza
        );
    }

    /**
     * Maneja la solicitud de confirmación para `/lumen clear`.
     *
     * @param context Contexto del comando.
     */
    private static void handleClearRequest(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(TranslationHandler.get("command.only_players"));
            return;
        }

        UUID playerId = player.getUniqueId();

        // Registra la solicitud de confirmación
        confirmationRequests.put(playerId, System.currentTimeMillis());
        player.sendMessage(TranslationHandler.get("command.clear.request"));
    }

    /**
     * Maneja la confirmación para `/lumen clear confirm`.
     *
     * @param context Contexto del comando.
     */
    private static void handleClearConfirm(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(TranslationHandler.get("command.only_players"));
            return;
        }

        UUID playerId = player.getUniqueId();

        // Verifica si hay una solicitud de confirmación activa
        if (!confirmationRequests.containsKey(playerId)) {
            player.sendMessage(TranslationHandler.get("command.clear.no_request"));
            return;
        }

        // Verifica si la solicitud ha expirado
        long requestTime = confirmationRequests.get(playerId);
        if (System.currentTimeMillis() - requestTime > CONFIRMATION_TIMEOUT) {
            confirmationRequests.remove(playerId);
            player.sendMessage(TranslationHandler.get("command.clear.expired"));
            return;
        }

        // Ejecuta la limpieza
        LightRegistry.clearAllBlocks();
        confirmationRequests.remove(playerId);

        player.sendMessage(TranslationHandler.get("command.clear.success"));
    }
}
