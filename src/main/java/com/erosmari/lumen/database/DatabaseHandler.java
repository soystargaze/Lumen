package com.erosmari.lumen.database;

import com.erosmari.lumen.utils.TranslationHandler;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseHandler {

    private static Logger logger;
    private static HikariDataSource dataSource;

    /**
     * Inicializa la conexión con SQLite.
     *
     * @param plugin El plugin principal.
     */
    public static void initialize(JavaPlugin plugin) {
        if (dataSource != null) {
            logger.warning("DatabaseHandler ya fue inicializado.");
            return;
        }

        logger = plugin.getLogger();

        try {
            initializeSQLite(plugin);
            createTables();
        } catch (Exception e) {
            logger.log(Level.SEVERE, TranslationHandler.get("database.init_error"), e);
            throw new IllegalStateException("Falló la inicialización de la base de datos.");
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
            throw new SQLException(TranslationHandler.get("database.sqlite.error_directory") + dbFolder.getAbsolutePath());
        }

        String dbFilePath = new File(dbFolder, "lumen.db").getAbsolutePath();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFilePath);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setPoolName("Lumen-SQLite");

        dataSource = new HikariDataSource(hikariConfig);
        logger.info(TranslationHandler.get("database.sqlite.success"));
    }

    /**
     * Crea las tablas necesarias para almacenar bloques iluminados.
     */
    private static void createTables() {
        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            String createIlluminatedBlocksTable = "CREATE TABLE IF NOT EXISTS illuminated_blocks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "world TEXT NOT NULL," +
                    "x INTEGER NOT NULL," +
                    "y INTEGER NOT NULL," +
                    "z INTEGER NOT NULL," +
                    "light_level INTEGER NOT NULL," +
                    "operation_id TEXT NOT NULL," +
                    "is_deleted BOOLEAN DEFAULT 0" +
                    ");";
            stmt.executeUpdate(createIlluminatedBlocksTable);

            // Tabla para áreas protegidas contra mobs
            String createProtectedAreasTable = "CREATE TABLE IF NOT EXISTS protected_areas (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "world TEXT NOT NULL," +
                    "x INTEGER NOT NULL," +
                    "y INTEGER NOT NULL," +
                    "z INTEGER NOT NULL," +
                    "radius INTEGER NOT NULL" +
                    ");";
            stmt.executeUpdate(createProtectedAreasTable);

            logger.info(TranslationHandler.get("database.tables.success"));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, TranslationHandler.get("database.tables.error"), e);
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
            throw new IllegalStateException("El pool de conexiones no está inicializado.");
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
            logger.info(TranslationHandler.get("database.close.success"));
        }
    }
}