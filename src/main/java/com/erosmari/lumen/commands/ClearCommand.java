package com.erosmari.lumen.commands;

import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.utils.LoggingUtils;
import com.erosmari.lumen.utils.RemoveLightUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class ClearCommand {

    private static final Map<UUID, Long> confirmationRequests = new HashMap<>();
    private static final long CONFIRMATION_TIMEOUT = 30_000; // 30 segundos

    public ClearCommand() {
    }

    /**
     * Registra el subcomando `/lumen clear` y su subcomando `/lumen clear confirm`.
     *
     * @return Nodo literal del comando para registrarlo en el comando principal.
     */
    public LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("clear")
                .requires(source -> source.getSender().hasPermission("lumen.clear"))
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
    private int handleClearRequest(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("command.only_players"));
            LoggingUtils.logTranslated("command.clear.only_players");
            return 0;
        }

        UUID playerId = player.getUniqueId();

        confirmationRequests.put(playerId, System.currentTimeMillis());
        LoggingUtils.sendAndLog(player,"command.clear.request");

        return 1;
    }

    /**
     * Maneja la confirmación para `/lumen clear confirm`.
     *
     * @param source Fuente del comando.
     * @return Código de éxito.
     */
    private int handleClearConfirm(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("command.only_players"));
            LoggingUtils.logTranslated("command.clear.only_players");
            return 0;
        }

        UUID playerId = player.getUniqueId();

        if (!confirmationRequests.containsKey(playerId)) {
            LoggingUtils.sendAndLog(player,"command.clear.no_request");
            return 0;
        }

        long requestTime = confirmationRequests.get(playerId);
        if (System.currentTimeMillis() - requestTime > CONFIRMATION_TIMEOUT) {
            confirmationRequests.remove(playerId);
            LoggingUtils.sendAndLog(player,"command.clear.expired");
            return 0;
        }

        List<Location> blocks = LightRegistry.getAllBlocks();

        // Usar RemoveLightUtils para eliminar bloques y registrar en CoreProtect
        int removedCount = blocks.stream()
                .mapToInt(location -> RemoveLightUtils.removeLightBlock(location) ? 1 : 0)
                .sum();

        LightRegistry.clearAllBlocks();
        confirmationRequests.remove(playerId);

        LoggingUtils.sendAndLog(player,"command.clear.success", removedCount);
        return 1;
    }
}