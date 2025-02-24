package com.erosmari.lumen.database;

import com.erosmari.lumen.utils.LoggingUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class LightRegistry {

    public static void addBlockAsync(Location location, int lightLevel, int operationId) {
        CompletableFuture.runAsync(() -> {
            if (lightLevel <= 0 || lightLevel > 15) {
                LoggingUtils.logTranslated("light_registry.error.invalid_light_level", lightLevel, location);
                return;
            }

            String query = "INSERT INTO illuminated_blocks (world, x, y, z, light_level, operation_id, is_deleted) VALUES (?, ?, ?, ?, ?, ?, 0);";

            try (Connection connection = DatabaseHandler.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {

                setBlockStatementParameters(statement, location, lightLevel, operationId);
                statement.executeUpdate();

            } catch (SQLException e) {
                LoggingUtils.logTranslated("light_registry.error.add_block", e.getMessage());
            }
        });
    }

    public static int registerOperation(UUID operationUuid, String description) {
        String sql = "INSERT INTO operations (operation_uuid, description) VALUES (?, ?)";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, operationUuid.toString());
            stmt.setString(2, description);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1); // Retorna el ID generado
                }
            }
        } catch (SQLException e) {
            LoggingUtils.logTranslated("database.operation.register.error", e.getMessage());
        }
        throw new IllegalStateException(TranslationHandler.get("database.operation.register.failed"));
    }

    public static void softDeleteBlocksByOperationId(int operationId) {
        String query = "UPDATE illuminated_blocks SET is_deleted = 1 WHERE operation_id = ?;";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, operationId);
            statement.executeUpdate();
            LoggingUtils.logTranslated("light_registry.info.blocks_soft_deleted", operationId);
        } catch (SQLException e) {
            LoggingUtils.logTranslated("light_registry.error.soft_delete", operationId, e.getMessage());
        }
    }

    public static void restoreSoftDeletedBlocksByOperationId(int operationId) {
        String query = "UPDATE illuminated_blocks SET is_deleted = 0 WHERE operation_id = ?;";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, operationId);
            statement.executeUpdate();
            LoggingUtils.logTranslated("light_registry.info.blocks_restored", operationId);
        } catch (SQLException e) {
            LoggingUtils.logTranslated("light_registry.error.restore", operationId, e.getMessage());
        }
    }

    public static Map<Location, Integer> getSoftDeletedBlocksWithLightLevelByOperationId(int operationId) {
        String query = "SELECT world, x, y, z, light_level FROM illuminated_blocks WHERE operation_id = ? AND is_deleted = 1;";
        Map<Location, Integer> blocksWithLightLevel = new HashMap<>();

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, operationId);

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
            LoggingUtils.logTranslated("light_registry.error.fetch_soft_deleted", operationId, e.getMessage());
        }

        return blocksWithLightLevel;
    }

    public static Integer getLastSoftDeletedOperationId() {
        String query = "SELECT operation_id FROM illuminated_blocks WHERE is_deleted = 1 ORDER BY id DESC LIMIT 1;";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getInt("operation_id");
            }
        } catch (SQLException e) {
            LoggingUtils.logTranslated("light_registry.error.fetch_last_soft_deleted", e.getMessage());
        }

        return null;
    }

    public static List<Location> getBlocksByOperationId(int operationId) {
        String query = "SELECT * FROM illuminated_blocks WHERE operation_id = ?;";
        List<Location> blocks = new ArrayList<>();

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, operationId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Location location = createLocationFromResultSet(resultSet);
                    if (location != null) {
                        blocks.add(location);
                    }
                }
            }
        } catch (SQLException e) {
            LoggingUtils.logTranslated("light_registry.error.fetch_blocks_by_operation", operationId, e.getMessage());
        }

        return blocks;
    }

    public static void removeBlocksByOperationId(int operationId) {
        String queryUpdate = "UPDATE illuminated_blocks SET is_deleted = 1 WHERE operation_id = ?;";
        String querySelect = "SELECT world, x, y, z FROM illuminated_blocks WHERE operation_id = ? AND is_deleted = 0;";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement selectStatement = connection.prepareStatement(querySelect);
             PreparedStatement updateStatement = connection.prepareStatement(queryUpdate)) {

            selectStatement.setInt(1, operationId);

            try (ResultSet resultSet = selectStatement.executeQuery()) {
                while (resultSet.next()) {
                    processResultSetToRemoveBlock(resultSet);
                }
            }

            updateStatement.setInt(1, operationId);
            updateStatement.executeUpdate();

            LoggingUtils.logTranslated("light_registry.info.blocks_removed", operationId);
        } catch (SQLException e) {
            LoggingUtils.logTranslated("light_registry.error.remove_blocks", operationId, e.getMessage());
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
            LoggingUtils.logTranslated("light_registry.error.fetch_blocks_in_range", e.getMessage());
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
            LoggingUtils.logTranslated("light_registry.error.fetch_all_blocks", e.getMessage());
        }

        return blocks;
    }

    public static List<Integer> getLastOperations(int count) {
        String query = "SELECT DISTINCT operation_id FROM illuminated_blocks WHERE is_deleted = 0 ORDER BY id DESC LIMIT ?;";
        List<Integer> operations = new ArrayList<>();

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, count);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    operations.add(resultSet.getInt("operation_id"));
                }
            }
        } catch (SQLException e) {
            LoggingUtils.logTranslated("light_registry.error.fetch_last_operations", count, e.getMessage());
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
            LoggingUtils.logTranslated("light_registry.info.all_blocks_removed");
        } catch (SQLException e) {
            LoggingUtils.logTranslated("light_registry.error.clear_all_blocks", e.getMessage());
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

        LoggingUtils.logTranslated("light_registry.warning.world_not_found", worldName);
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

    public static void addBlocksAsync(List<Location> locations, int lightLevel, int operationId) {
        CompletableFuture.runAsync(() -> {
            String query = "INSERT INTO illuminated_blocks (world, x, y, z, light_level, operation_id, is_deleted) VALUES (?, ?, ?, ?, ?, ?, 0);";

            try (Connection connection = DatabaseHandler.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {

                connection.setAutoCommit(false);

                for (Location location : locations) {
                    if (lightLevel <= 0 || lightLevel > 15) {
                        LoggingUtils.logTranslated("light_registry.error.invalid_light_level", lightLevel, location);
                        continue;
                    }

                    setBlockStatementParameters(statement, location, lightLevel, operationId);
                    statement.addBatch();
                }

                statement.executeBatch();
                connection.commit();
                LoggingUtils.logTranslated("light_registry.info.blocks_added", locations.size(), operationId);
            } catch (SQLException e) {
                LoggingUtils.logTranslated("light_registry.error.add_blocks", e.getMessage());
            }
        });
    }

    private static void setBlockStatementParameters(PreparedStatement statement, Location location, int lightLevel, int operationId) throws SQLException {
        statement.setString(1, location.getWorld().getName());
        statement.setInt(2, location.getBlockX());
        statement.setInt(3, location.getBlockY());
        statement.setInt(4, location.getBlockZ());
        statement.setInt(5, lightLevel);
        statement.setInt(6, operationId);
    }
}
