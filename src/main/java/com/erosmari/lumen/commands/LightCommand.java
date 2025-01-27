package com.erosmari.lumen.commands;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.lights.LightHandler;
import com.erosmari.lumen.utils.TranslationHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class LightCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("light")
                .requires(source -> source.getSender().hasPermission("lumen.light"))
                .then(
                        Commands.argument("range", IntegerArgumentType.integer(1, 150)) // Tamaño del área en bloques
                                .then(
                                        Commands.argument("light_level", IntegerArgumentType.integer(0, 15)) // Nivel de luz
                                                .then(
                                                        Commands.argument("include_skylight", BoolArgumentType.bool()) // Luz natural opcional
                                                                .executes(ctx -> handleLightCommand(
                                                                        ctx.getSource(),
                                                                        ctx.getArgument("range", Integer.class),
                                                                        ctx.getArgument("light_level", Integer.class),
                                                                        ctx.getArgument("include_skylight", Boolean.class)
                                                                ))
                                                )
                                                .executes(ctx -> handleLightCommand(
                                                        ctx.getSource(),
                                                        ctx.getArgument("range", Integer.class),
                                                        ctx.getArgument("light_level", Integer.class),
                                                        false // Valor predeterminado
                                                ))
                                )
                )
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Component.text(TranslationHandler.get("command.light.usage")).color(NamedTextColor.RED));
                    return 0;
                });
    }

    private static int handleLightCommand(CommandSourceStack source, int areaBlocks, int lightLevel, boolean includeSkylight) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.only_players")).color(NamedTextColor.RED));
            return 0;
        }

        // Verificar si el jugador tiene permiso
        if (!player.hasPermission("lumen.light")) {
            player.sendMessage(Component.text(TranslationHandler.get("command.no_permission")).color(NamedTextColor.RED));
            return 0;
        }

        if (lightLevel < 0 || lightLevel > 15) {
            player.sendMessage(Component.text(TranslationHandler.get("command.light.invalid_level")).color(NamedTextColor.RED));
            return 0;
        }

        // Registrar la operación en la base de datos y obtener el ID incremental
        int operationId = LightRegistry.registerOperation(java.util.UUID.randomUUID(), "Generación de luces");


        LightHandler lightHandler = new LightHandler(Lumen.getInstance());
        lightHandler.placeLights(player, areaBlocks, lightLevel, includeSkylight, operationId);

        player.sendMessage(Component.text(TranslationHandler.getFormatted("command.light.success", lightLevel, operationId)));
        return 1;
    }
}