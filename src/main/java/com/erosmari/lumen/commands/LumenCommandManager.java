package com.erosmari.lumen.commands;

import com.erosmari.lumen.Lumen;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;

@SuppressWarnings("UnstableApiUsage")
public class LumenCommandManager {

    private final Lumen plugin;

    public LumenCommandManager(Lumen plugin) {
        this.plugin = plugin;
    }

    /**
     * Registra todos los comandos principales y sus subcomandos.
     */
    public void registerCommands() {
        LifecycleEventManager<@org.jetbrains.annotations.NotNull Plugin> manager = plugin.getLifecycleManager();
        // Registrar comandos usando el evento COMMANDS
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            // Registro del comando principal `/lumen` y sus subcomandos
            commands.register(
                    Commands.literal("lumen")
                            .executes(ctx -> {
                                CommandSourceStack source = ctx.getSource();
                                source.getSender().sendMessage("Uso del comando /lumen:");
                                return 1; // Comando ejecutado con éxito
                            })
                            // Añadir subcomandos
                            .then(LightCommand.register())
                            .then(CancelCommand.register())
                            .then(UndoCommand.register())
                            .then(RedoCommand.register())
                            .then(ClearCommand.register())
                            .then(RemoveCommand.register())
                            .then(GiveCommand.register())
                            .then(ReloadCommand.register(plugin))
                            .build()
            );
        });
    }
}