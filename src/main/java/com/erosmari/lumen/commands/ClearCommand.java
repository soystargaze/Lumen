package com.erosmari.lumen.commands;

import com.erosmari.lumen.connections.CoreProtectCompatibility;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.utils.RemoveLightUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@SuppressWarnings("UnstableApiUsage")
public class ClearCommand {

    private static final Map<UUID, Long> confirmationRequests = new HashMap<>();
    private static final long CONFIRMATION_TIMEOUT = 30_000; // 30 segundos

    private final CoreProtectCompatibility coreProtectCompatibility;
    private final Logger logger;

    public ClearCommand(CoreProtectCompatibility coreProtectCompatibility, Logger logger) {
        this.coreProtectCompatibility = coreProtectCompatibility;
        this.logger = logger;
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
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.only_players")).color(NamedTextColor.RED));
            return 0;
        }

        UUID playerId = player.getUniqueId();

        confirmationRequests.put(playerId, System.currentTimeMillis());
        player.sendMessage(Component.text(TranslationHandler.get("command.clear.request")).color(NamedTextColor.YELLOW));

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
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.only_players")).color(NamedTextColor.RED));
            return 0;
        }

        UUID playerId = player.getUniqueId();

        if (!confirmationRequests.containsKey(playerId)) {
            player.sendMessage(Component.text(TranslationHandler.get("command.clear.no_request")).color(NamedTextColor.RED));
            return 0;
        }

        long requestTime = confirmationRequests.get(playerId);
        if (System.currentTimeMillis() - requestTime > CONFIRMATION_TIMEOUT) {
            confirmationRequests.remove(playerId);
            player.sendMessage(Component.text(TranslationHandler.get("command.clear.expired")).color(NamedTextColor.RED));
            return 0;
        }

        List<Location> blocks = LightRegistry.getAllBlocks();

        // Usar RemoveLightUtils para eliminar bloques y registrar en CoreProtect
        int removedCount = blocks.stream()
                .mapToInt(location -> RemoveLightUtils.removeLightBlock(logger, player, location, coreProtectCompatibility) ? 1 : 0)
                .sum();

        LightRegistry.clearAllBlocks();
        confirmationRequests.remove(playerId);

        player.sendMessage(Component.text(TranslationHandler.getFormatted("command.clear.success", removedCount)).color(NamedTextColor.GREEN));
        return 1;
    }
}