package com.erosmari.lumen.commands;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.utils.TranslationHandler;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;

@SuppressWarnings("UnstableApiUsage")
public class LumenCommandManager {

    private final Lumen plugin;
    private final RemoveCommand removeCommand;
    private final UndoCommand undoCommand;
    private final ClearCommand clearCommand;
    private final RedoCommand redoCommand;

    public LumenCommandManager(Lumen plugin) {
        this.plugin = plugin;
        this.removeCommand = new RemoveCommand(plugin);
        this.undoCommand = new UndoCommand(plugin);
        this.clearCommand = new ClearCommand();
        this.redoCommand = new RedoCommand(plugin);
    }

    /**
     * Registra todos los comandos principales y sus subcomandos.
     */
    public void registerCommands() {
        LifecycleEventManager<@org.jetbrains.annotations.NotNull Plugin> manager = plugin.getLifecycleManager();
        // Registrar comandos usando el evento COMMANDS
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register(
                    Commands.literal("lumen")
                            .requires(source -> source.getSender().hasPermission("lumen.use"))
                            .executes(ctx -> {
                                CommandSourceStack source = ctx.getSource();
                                source.getSender().sendMessage(Component.text(TranslationHandler.get("command.usage")));
                                return 1;
                            })
                            .then(LightCommand.register())
                            .then(CancelCommand.register())
                            .then(undoCommand.register())
                            .then(redoCommand.register())
                            .then(clearCommand.register())
                            .then(removeCommand.register())
                            .then(GiveCommand.register())
                            .then(ReloadCommand.register(plugin))
                            .build()
            );
            commands.register(
                    Commands.literal("l")
                            .requires(source -> source.getSender().hasPermission("lumen.use"))
                            .executes(ctx -> {
                                CommandSourceStack source = ctx.getSource();
                                source.getSender().sendMessage(Component.text(TranslationHandler.get("command.usage")));
                                return 1;
                            })
                            .then(LightCommand.register())
                            .then(CancelCommand.register())
                            .then(undoCommand.register())
                            .then(redoCommand.register())
                            .then(clearCommand.register())
                            .then(removeCommand.register())
                            .then(GiveCommand.register())
                            .then(ReloadCommand.register(plugin))
                            .build()
            );
            commands.register(
                    Commands.literal("lu")
                            .requires(source -> source.getSender().hasPermission("lumen.use"))
                            .executes(ctx -> {
                                CommandSourceStack source = ctx.getSource();
                                source.getSender().sendMessage(Component.text(TranslationHandler.get("command.usage")));
                                return 1;
                            })
                            .then(LightCommand.register())
                            .then(CancelCommand.register())
                            .then(undoCommand.register())
                            .then(redoCommand.register())
                            .then(clearCommand.register())
                            .then(removeCommand.register())
                            .then(GiveCommand.register())
                            .then(ReloadCommand.register(plugin))
                            .build()
            );
        });
    }
}