package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import com.erosmari.lumen.utils.TranslationHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class ReloadCommand {

    private final JavaPlugin plugin;

    public ReloadCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registra el comando /lumen reload.
     *
     * @param commandManager El CommandManager para registrar el comando.
     * @param parentBuilder  El constructor del comando principal.
     * @param plugin         El plugin principal.
     */
    public static void register(CommandManager<CommandSender> commandManager, Command.Builder<CommandSender> parentBuilder, JavaPlugin plugin) {
        Command.Builder<CommandSender> reloadCommand = parentBuilder
                .literal("reload")
                .permission("lumen.reload")
                .handler(context -> new ReloadCommand(plugin).execute(context));

        commandManager.command(reloadCommand.build());
    }

    /**
     * Manejo del comando "/lumen reload".
     */
    public void execute(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();

        // Validar permisos del remitente
        if (!sender.hasPermission("lumen.reload")) {
            sendMessage(sender, "reload.no_permission", NamedTextColor.RED);
            return;
        }

        try {
            // Recargar configuración y traducciones
            reloadConfig();
            int loadedTranslations = reloadTranslations();

            // Mensajes de éxito
            sendMessage(sender, "reload.success", NamedTextColor.GREEN, loadedTranslations);
            logInfo(sender.getName(), loadedTranslations);
        } catch (Exception e) {
            // Manejo de errores
            logError(sender.getName(), e);
            sendMessage(sender, "reload.error", NamedTextColor.RED);
        }
    }

    /**
     * Recarga la configuración del plugin.
     */
    private void reloadConfig() {
        plugin.reloadConfig();
    }

    /**
     * Recarga las traducciones del plugin.
     *
     * @return Número de traducciones cargadas.
     */
    private int reloadTranslations() {
        TranslationHandler.clearTranslations();
        TranslationHandler.loadTranslations(plugin, plugin.getConfig().getString("language", "es_es"));
        return TranslationHandler.getLoadedTranslationsCount();
    }

    /**
     * Envía un mensaje al remitente.
     *
     * @param sender       El remitente del mensaje.
     * @param key          Clave de traducción.
     * @param color        Color del mensaje.
     * @param placeholders Opcionales, reemplazos dinámicos en el mensaje.
     */
    private void sendMessage(CommandSender sender, String key, NamedTextColor color, Object... placeholders) {
        sender.sendMessage(Component.text(TranslationHandler.getFormatted(key, placeholders)).color(color));
    }

    /**
     * Registra un mensaje informativo en los logs.
     *
     * @param placeholders Reemplazos dinámicos en el mensaje.
     */
    private void logInfo(Object... placeholders) {
        plugin.getLogger().info(TranslationHandler.getFormatted("reload.success_log", placeholders));
    }

    /**
     * Registra un mensaje de error en los logs.
     *
     * @param playerName Nombre del jugador que ejecutó el comando.
     * @param exception  La excepción capturada.
     */
    private void logError(String playerName, Exception exception) {
        plugin.getLogger().log(Level.SEVERE, TranslationHandler.getFormatted("reload.error_log", playerName), exception);
    }
}
