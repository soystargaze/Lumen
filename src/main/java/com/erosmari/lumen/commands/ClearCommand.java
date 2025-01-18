package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import com.erosmari.lumen.database.LightRegistry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClearCommand {

    private static final Map<UUID, Long> confirmationRequests = new HashMap<>(); // Mapa de confirmaciones pendientes
    private static final long CONFIRMATION_TIMEOUT = 30_000; // Tiempo límite para confirmar (30 segundos)

    /**
     * Registra el comando `/lumen clear`.
     *
     * @param commandManager El administrador de comandos.
     * @param parentBuilder  El constructor del comando principal.
     */
    public static void register(CommandManager<CommandSender> commandManager, Command.Builder<CommandSender> parentBuilder) {
        commandManager.command(
                parentBuilder.literal("clear")
                        .permission("lumen.clear")
                        .handler(ClearCommand::handleClearRequest) // Solicitar confirmación
        );

        commandManager.command(
                parentBuilder.literal("clear")
                        .literal("confirm")
                        .permission("lumen.clear")
                        .handler(ClearCommand::handleClearConfirm) // Confirmar y ejecutar
        );
    }

    /**
     * Solicita confirmación para ejecutar `/lumen clear`.
     */
    private static void handleClearRequest(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo los jugadores pueden usar este comando.");
            return;
        }

        UUID playerId = player.getUniqueId();

        // Registra la solicitud de confirmación
        confirmationRequests.put(playerId, System.currentTimeMillis());
        player.sendMessage("§e¿Estás seguro de que deseas eliminar todos los bloques iluminados?");
        player.sendMessage("§eEscribe §a/lumen clear confirm §epara confirmar. Tienes 30 segundos.");
    }

    /**
     * Maneja la confirmación de `/lumen clear confirm`.
     */
    private static void handleClearConfirm(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo los jugadores pueden usar este comando.");
            return;
        }

        UUID playerId = player.getUniqueId();

        // Verifica si hay una solicitud de confirmación activa
        if (!confirmationRequests.containsKey(playerId)) {
            player.sendMessage("§cNo tienes ninguna solicitud de confirmación activa.");
            return;
        }

        // Verifica si la solicitud ha expirado
        long requestTime = confirmationRequests.get(playerId);
        if (System.currentTimeMillis() - requestTime > CONFIRMATION_TIMEOUT) {
            confirmationRequests.remove(playerId);
            player.sendMessage("§cTu solicitud de confirmación ha expirado.");
            return;
        }

        // Ejecuta la limpieza
        LightRegistry.clearAllBlocks();
        confirmationRequests.remove(playerId);

        player.sendMessage("§aTodos los bloques iluminados han sido eliminados.");
    }
}
