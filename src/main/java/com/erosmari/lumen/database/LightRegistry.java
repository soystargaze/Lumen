package com.erosmari.lumen.database;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LightRegistry {

    private static final Logger logger = Logger.getLogger("Lumen-LightRegistry");

    /**
     * Registra un bloque iluminado en la base de datos.
     *
     * @param location   Ubicación del bloque.
     * @param lightLevel Nivel de luz del bloque.
     * @param operationId Identificador de la operación.
     */
    public static void addBlock(Location location, int lightLevel, String operationId) {
        String query = "INSERT INTO illuminated_blocks (world, x, y, z, light_level, operation_id, is_deleted) VALUES (?, ?, ?, ?, ?, ?, 0);";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, location.getWorld().getName());
            statement.setInt(2, location.getBlockX());
            statement.setInt(3, location.getBlockY());
            statement.setInt(4, location.getBlockZ());
            statement.setInt(5, lightLevel);
            statement.setString(6, operationId);

            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al registrar el bloque en la base de datos.", e);
        }
    }

    /**
     * Marca los bloques de una operación como eliminados (soft delete).
     *
     * @param operationId El identificador de la operación.
     */
    public static void softDeleteBlocksByOperationId(String operationId) {
        String query = "UPDATE illuminated_blocks SET is_deleted = 1 WHERE operation_id = ?;";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, operationId);
            statement.executeUpdate();
            logger.info("Bloques de la operación " + operationId + " marcados como eliminados.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al marcar como eliminados los bloques de la operación: " + operationId, e);
        }
    }

    /**
     * Obtiene todos los bloques iluminados almacenados en la base de datos.
     *
     * @return Una lista de ubicaciones de bloques iluminados.
     */
    public static List<Location> getAllBlocks() {
        String query = "SELECT * FROM illuminated_blocks WHERE is_deleted = 0;";
        List<Location> blocks = new ArrayList<>();

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Location location = createLocationFromResultSet(resultSet);
                if (location != null) {
                    blocks.add(location);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al obtener todos los bloques iluminados.", e);
        }

        return blocks;
    }

    /**
     * Obtiene los bloques por operation_id, incluso si están marcados como eliminados.
     *
     * @param operationId El identificador de la operación.
     * @return Una lista de ubicaciones.
     */
    public static List<Location> getBlocksByOperationId(String operationId) {
        String query = "SELECT * FROM illuminated_blocks WHERE operation_id = ?;";
        List<Location> blocks = new ArrayList<>();

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, operationId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Location location = createLocationFromResultSet(resultSet);
                    if (location != null) {
                        blocks.add(location);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al obtener los bloques por operation_id: " + operationId, e);
        }

        return blocks;
    }

    private static void processResultSetToRemoveBlock(ResultSet resultSet) throws SQLException {
        String worldName = resultSet.getString("world");
        int x = resultSet.getInt("x");
        int y = resultSet.getInt("y");
        int z = resultSet.getInt("z");

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Location location = new Location(world, x, y, z);

            // Reemplazar el bloque de luz con aire
            if (location.getBlock().getType() == Material.LIGHT) {
                location.getBlock().setType(Material.AIR);
            }
        } else {
            logger.warning("El mundo '" + worldName + "' no está cargado o no existe.");
        }
    }

    /**
     * Marca los bloques como eliminados (soft delete) en la base de datos y los elimina del mundo.
     *
     * @param operationId El identificador de la operación.
     */
    public static void removeBlocksByOperationId(String operationId) {
        String queryUpdate = "UPDATE illuminated_blocks SET is_deleted = 1 WHERE operation_id = ?;";
        String querySelect = "SELECT world, x, y, z FROM illuminated_blocks WHERE operation_id = ? AND is_deleted = 0;";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement selectStatement = connection.prepareStatement(querySelect);
             PreparedStatement updateStatement = connection.prepareStatement(queryUpdate)) {

            selectStatement.setString(1, operationId);

            try (ResultSet resultSet = selectStatement.executeQuery()) {
                while (resultSet.next()) {
                    processResultSetToRemoveBlock(resultSet);
                }
            }

            updateStatement.setString(1, operationId);
            updateStatement.executeUpdate();

            logger.info("Bloques marcados como eliminados y eliminados del mundo para operation_id: " + operationId);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al marcar los bloques como eliminados y eliminarlos del mundo para operation_id: " + operationId, e);
        }
    }

    public static String getLastOperationId() {
        String query = "SELECT operation_id FROM illuminated_blocks WHERE is_deleted = 0 ORDER BY id DESC LIMIT 1;";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getString("operation_id");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al obtener el último operation_id.", e);
        }

        return null;
    }

    public static int getLightLevel(Location location) {
        String query = "SELECT light_level FROM illuminated_blocks WHERE world = ? AND x = ? AND y = ? AND z = ? AND is_deleted = 0;";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, location.getWorld().getName());
            statement.setInt(2, location.getBlockX());
            statement.setInt(3, location.getBlockY());
            statement.setInt(4, location.getBlockZ());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("light_level");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al obtener el nivel de luz para la ubicación: " + location, e);
        }

        return 0;
    }

    /**
     * Obtiene todos los bloques dentro de un rango específico.
     *
     * @param center Centro del rango.
     * @param range  Radio del rango.
     * @return Lista de bloques dentro del rango.
     */
    public static List<Location> getBlocksInRange(Location center, int range) {
        String query = "SELECT * FROM illuminated_blocks WHERE is_deleted = 0 AND world = ? AND x BETWEEN ? AND ? AND y BETWEEN ? AND ? AND z BETWEEN ? AND ?;";
        List<Location> blocks = new ArrayList<>();

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            setQueryParameters(statement, center, range);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Location location = createLocationFromResultSet(resultSet);
                    if (location != null) {
                        blocks.add(location);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al obtener los bloques en el rango especificado.", e);
        }

        return blocks;
    }

    /**
     * Elimina todos los bloques iluminados del servidor y de la base de datos.
     */
    @SuppressWarnings("SqlWithoutWhere")
    public static void clearAllBlocks() {
        String querySelect = "SELECT world, x, y, z FROM illuminated_blocks WHERE is_deleted = 0;";
        String queryUpdate = "UPDATE illuminated_blocks SET is_deleted = 1;";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement selectStatement = connection.prepareStatement(querySelect);
             PreparedStatement updateStatement = connection.prepareStatement(queryUpdate);
             ResultSet resultSet = selectStatement.executeQuery()) {

            while (resultSet.next()) {
                processResultSetToRemoveBlock(resultSet);
            }

            updateStatement.executeUpdate();
            logger.info("Todos los bloques iluminados han sido eliminados del mundo y marcados como eliminados en la base de datos.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al eliminar todos los bloques iluminados.", e);
        }
    }

    private static Location createLocationFromResultSet(ResultSet resultSet) throws SQLException {
        String worldName = resultSet.getString("world");
        int x = resultSet.getInt("x");
        int y = resultSet.getInt("y");
        int z = resultSet.getInt("z");

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return new Location(world, x, y, z);
        }

        logger.warning("El mundo '" + worldName + "' no está cargado o no existe.");
        return null;
    }

    private static void setQueryParameters(PreparedStatement statement, Location center, int range) throws SQLException {
        statement.setString(1, center.getWorld().getName());
        statement.setInt(2, center.getBlockX() - range);
        statement.setInt(3, center.getBlockX() + range);
        statement.setInt(4, center.getBlockY() - range);
        statement.setInt(5, center.getBlockY() + range);
        statement.setInt(6, center.getBlockZ() - range);
        statement.setInt(7, center.getBlockZ() + range);
    }
}