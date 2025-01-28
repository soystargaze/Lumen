package com.erosmari.lumen.database;

import com.erosmari.lumen.utils.LoggingUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;

public class DatabaseHandler {

    private static HikariDataSource dataSource;

    /**
     * Inicializa la conexión con SQLite.
     *
     * @param plugin El plugin principal.
     */
    public static void initialize(JavaPlugin plugin) {
        if (dataSource != null) {
            LoggingUtils.logTranslated("database.init.already_initialized");
            return;
        }

        try {
            initializeSQLite(plugin);
            createTables();
        } catch (Exception e) {
            LoggingUtils.logTranslated("database.init.error", e.getMessage());
            throw new IllegalStateException(TranslationHandler.get("database.init.failed"));
        }
    }

    /**
     * Configura y conecta con SQLite.
     *
     * @param plugin El plugin principal.
     * @throws SQLException Si ocurre un error al configurar SQLite.
     */
    private static void initializeSQLite(JavaPlugin plugin) throws SQLException {
        File dbFolder = new File(plugin.getDataFolder(), "Data");
        if (!dbFolder.exists() && !dbFolder.mkdirs()) {
            throw new SQLException(TranslationHandler.getFormatted("database.sqlite.error_directory", dbFolder.getAbsolutePath()));
        }

        String dbFilePath = new File(dbFolder, "lumen.db").getAbsolutePath();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFilePath);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setPoolName("Lumen-SQLite");

        dataSource = new HikariDataSource(hikariConfig);
    }

    /**
     * Crea las tablas necesarias para almacenar bloques iluminados.
     */
    private static void createTables() {
        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            // Crear tabla illuminated_blocks
            String createIlluminatedBlocksTable = "CREATE TABLE IF NOT EXISTS illuminated_blocks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "world TEXT NOT NULL," +
                    "x INTEGER NOT NULL," +
                    "y INTEGER NOT NULL," +
                    "z INTEGER NOT NULL," +
                    "light_level INTEGER NOT NULL," +
                    "operation_id INTEGER NOT NULL," +
                    "is_deleted BOOLEAN DEFAULT 0," +
                    "FOREIGN KEY(operation_id) REFERENCES operations(id)," +
                    "UNIQUE(world, x, y, z, operation_id) ON CONFLICT IGNORE" +
                    ");";
            stmt.executeUpdate(createIlluminatedBlocksTable);

            String createProtectedAreasTable = "CREATE TABLE IF NOT EXISTS protected_areas (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "world TEXT NOT NULL," +
                    "x INTEGER NOT NULL," +
                    "y INTEGER NOT NULL," +
                    "z INTEGER NOT NULL," +
                    "radius INTEGER NOT NULL" +
                    ");";
            stmt.executeUpdate(createProtectedAreasTable);

            String createOperationsTable = "CREATE TABLE IF NOT EXISTS operations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "operation_uuid TEXT NOT NULL UNIQUE," +
                    "description TEXT" +
                    ");";
            stmt.executeUpdate(createOperationsTable);

        } catch (SQLException e) {
            LoggingUtils.logTranslated("database.tables.error", e.getMessage());
        }
    }

    /**
     * Retorna una conexión del pool.
     *
     * @return Conexión activa.
     * @throws SQLException Si ocurre un error al obtener la conexión.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException(TranslationHandler.get("database.connection.uninitialized"));
        }
        return dataSource.getConnection();
    }

    /**
     * Cierra el pool de conexiones.
     */
    public static void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
            LoggingUtils.logTranslated("database.close.success");
        }
    }
}
