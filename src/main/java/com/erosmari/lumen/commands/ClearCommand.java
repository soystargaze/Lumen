package com.erosmari.lumen.commands;

import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.utils.TranslationHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class ClearCommand {

    private static final Map<UUID, Long> confirmationRequests = new HashMap<>(); // Solicitudes de confirmación
    private static final long CONFIRMATION_TIMEOUT = 30_000; // Tiempo límite para confirmar (30 segundos)

    /**
     * Registra el subcomando `/lumen clear` y su subcomando `/lumen clear confirm` en el sistema nativo de Paper.
     *
     * @return Nodo literal del comando para registrarlo en el comando principal.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("clear")
                .requires(source -> source.getSender().hasPermission("lumen.clear")) // Validar permisos
                .executes(ctx -> handleClearRequest(ctx.getSource())) // Solicita confirmación
                .then(
                        Commands.literal("confirm")
                                .executes(ctx -> handleClearConfirm(ctx.getSource())) // Ejecuta la limpieza
                );
    }

    /**
     * Maneja la solicitud de confirmación para `/lumen clear`.
     *
     * @param source Fuente del comando.
     * @return Código de éxito.
     */
    private static int handleClearRequest(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.only_players")).color(NamedTextColor.RED));
            return 0;
        }

        UUID playerId = player.getUniqueId();

        // Registra la solicitud de confirmación
        confirmationRequests.put(playerId, System.currentTimeMillis());
        player.sendMessage(Component.text(TranslationHandler.get("command.clear.request")).color(NamedTextColor.YELLOW));

        return 1; // Comando ejecutado con éxito
    }

    /**
     * Maneja la confirmación para `/lumen clear confirm`.
     *
     * @param source Fuente del comando.
     * @return Código de éxito.
     */
    private static int handleClearConfirm(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.only_players")).color(NamedTextColor.RED));
            return 0;
        }

        UUID playerId = player.getUniqueId();

        // Verifica si hay una solicitud de confirmación activa
        if (!confirmationRequests.containsKey(playerId)) {
            player.sendMessage(Component.text(TranslationHandler.get("command.clear.no_request")).color(NamedTextColor.RED));
            return 0;
        }

        // Verifica si la solicitud ha expirado
        long requestTime = confirmationRequests.get(playerId);
        if (System.currentTimeMillis() - requestTime > CONFIRMATION_TIMEOUT) {
            confirmationRequests.remove(playerId);
            player.sendMessage(Component.text(TranslationHandler.get("command.clear.expired")).color(NamedTextColor.RED));
            return 0;
        }

        // Ejecuta la limpieza
        LightRegistry.clearAllBlocks();
        confirmationRequests.remove(playerId);

        player.sendMessage(Component.text(TranslationHandler.get("command.clear.success")).color(NamedTextColor.GREEN));
        return 1; // Comando ejecutado con éxito
    }
}