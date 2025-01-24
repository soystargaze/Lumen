package com.erosmari.lumen.database;

import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LightRegistry {

    private static final Logger logger = Logger.getLogger("Lumen-LightRegistry");

    public static void addBlockAsync(Location location, int lightLevel, String operationId) {
        CompletableFuture.runAsync(() -> {
            if (lightLevel <= 0 || lightLevel > 15) {
                logger.warning(TranslationHandler.getFormatted("light_registry.error.invalid_light_level", lightLevel, location));
                return;
            }

            String query = "INSERT INTO illuminated_blocks (world, x, y, z, light_level, operation_id, is_deleted) VALUES (?, ?, ?, ?, ?, ?, 0);";

            try (Connection connection = DatabaseHandler.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {

                setBlockStatementParameters(statement, location, lightLevel, operationId);
                statement.executeUpdate();

                logger.info(TranslationHandler.getFormatted("light_registry.info.block_added", location, lightLevel, operationId));
            } catch (SQLException e) {
                logger.log(Level.SEVERE, TranslationHandler.get("light_registry.error.add_block"), e);
            }
        });
    }

    public static void softDeleteBlocksByOperationId(String operationId) {
        String query = "UPDATE illuminated_blocks SET is_deleted = 1 WHERE operation_id = ?;";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, operationId);
            statement.executeUpdate();
            logger.info(TranslationHandler.getFormatted("light_registry.info.blocks_soft_deleted", operationId));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, TranslationHandler.getFormatted("light_registry.error.soft_delete", operationId), e);
        }
    }

    public static void restoreSoftDeletedBlocksByOperationId(String operationId) {
        String query = "UPDATE illuminated_blocks SET is_deleted = 0 WHERE operation_id = ?;";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, operationId);
            statement.executeUpdate();
            logger.info(TranslationHandler.getFormatted("light_registry.info.blocks_restored", operationId));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, TranslationHandler.getFormatted("light_registry.error.restore", operationId), e);
        }
    }

    public static Map<Location, Integer> getSoftDeletedBlocksWithLightLevelByOperationId(String operationId) {
        String query = "SELECT world, x, y, z, light_level FROM illuminated_blocks WHERE operation_id = ? AND is_deleted = 1;";
        Map<Location, Integer> blocksWithLightLevel = new HashMap<>();

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, operationId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Location location = createLocationFromResultSet(resultSet);
                    if (location != null) {
                        int lightLevel = resultSet.getInt("light_level");
                        blocksWithLightLevel.put(location, lightLevel);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, TranslationHandler.getFormatted("light_registry.error.fetch_soft_deleted", operationId), e);
        }

        return blocksWithLightLevel;
    }

    public static String getLastSoftDeletedOperationId() {
        String query = "SELECT operation_id FROM illuminated_blocks WHERE is_deleted = 1 ORDER BY id DESC LIMIT 1;";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getString("operation_id");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, TranslationHandler.get("light_registry.error.fetch_last_soft_deleted"), e);
        }

        return null;
    }

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
            logger.log(Level.SEVERE, TranslationHandler.getFormatted("light_registry.error.fetch_blocks_by_operation", operationId), e);
        }

        return blocks;
    }

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

            logger.info(TranslationHandler.getFormatted("light_registry.info.blocks_removed", operationId));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, TranslationHandler.getFormatted("light_registry.error.remove_blocks", operationId), e);
        }
    }

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
            logger.log(Level.SEVERE, TranslationHandler.get("light_registry.error.fetch_blocks_in_range"), e);
        }

        return blocks;
    }

    public static List<Location> getAllBlocks() {
        String query = "SELECT world, x, y, z FROM illuminated_blocks WHERE is_deleted = 0;";
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
            logger.log(Level.SEVERE, TranslationHandler.get("light_registry.error.fetch_all_blocks"), e);
        }

        return blocks;
    }

    public static List<String> getLastOperations(int count) {
        String query = "SELECT DISTINCT operation_id FROM illuminated_blocks WHERE is_deleted = 0 ORDER BY id DESC LIMIT ?;";
        List<String> operations = new ArrayList<>();

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, count);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    operations.add(resultSet.getString("operation_id"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, TranslationHandler.getFormatted("light_registry.error.fetch_last_operations", count), e);
        }

        return operations;
    }

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
            logger.info(TranslationHandler.get("light_registry.info.all_blocks_removed"));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, TranslationHandler.get("light_registry.error.clear_all_blocks"), e);
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

        logger.warning(TranslationHandler.getFormatted("light_registry.warning.world_not_found", worldName));
        return null;
    }

    private static void processResultSetToRemoveBlock(ResultSet resultSet) throws SQLException {
        Location location = createLocationFromResultSet(resultSet);
        if (location != null && location.getBlock().getType() == Material.LIGHT) {
            location.getBlock().setType(Material.AIR);
        }
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

    public static void addBlocksAsync(List<Location> locations, int lightLevel, String operationId) {
        CompletableFuture.runAsync(() -> {
            String query = "INSERT INTO illuminated_blocks (world, x, y, z, light_level, operation_id, is_deleted) VALUES (?, ?, ?, ?, ?, ?, 0);";

            try (Connection connection = DatabaseHandler.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {

                connection.setAutoCommit(false); // Inicia la transacción

                for (Location location : locations) {
                    if (lightLevel <= 0 || lightLevel > 15) {
                        logger.warning(TranslationHandler.getFormatted("light_registry.error.invalid_light_level", lightLevel, location));
                        continue; // Salta la ubicación con nivel de luz no válido
                    }

                    setBlockStatementParameters(statement, location, lightLevel, operationId);
                    statement.addBatch(); // Agrega al lote
                }

                statement.executeBatch(); // Ejecuta el lote
                connection.commit(); // Confirma la transacción

                logger.info(TranslationHandler.getFormatted("light_registry.info.blocks_added", locations.size(), operationId));
            } catch (SQLException e) {
                logger.log(Level.SEVERE, TranslationHandler.get("light_registry.error.add_blocks"), e);
            }
        });
    }

    private static void setBlockStatementParameters(PreparedStatement statement, Location location, int lightLevel, String operationId) throws SQLException {
        statement.setString(1, location.getWorld().getName());
        statement.setInt(2, location.getBlockX());
        statement.setInt(3, location.getBlockY());
        statement.setInt(4, location.getBlockZ());
        statement.setInt(5, lightLevel);
        statement.setString(6, operationId);
    }
}
