package com.grinderwolf.swm.plugin.loaders.sql;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.plugin.config.DatasourcesConfig;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.plugin.loaders.UpdatableLoader;
import com.grinderwolf.swm.plugin.log.Logging;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SqlLoader extends UpdatableLoader {

    // World locking executor service
    private static final ScheduledExecutorService SERVICE = Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder()
            .setNameFormat("SWM SQL Lock Pool Thread #%1$d").build());

    private static final int CURRENT_DB_VERSION = 1;

    // Database version handling queries
    private static final String CREATE_VERSIONING_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS database_version (" +
            "id INT PRIMARY KEY, version INT)";
    private static final String INSERT_VERSION_QUERY_MYSQL = "INSERT INTO database_version (id, version) VALUES (1, ?) " +
            "ON DUPLICATE KEY UPDATE version = VALUES(version)";
    private static final String INSERT_VERSION_QUERY_POSTGRES = "INSERT INTO database_version (id, version) VALUES (1, ?) " +
            "ON CONFLICT (id) DO UPDATE SET version = EXCLUDED.version";
    private static final String GET_VERSION_QUERY = "SELECT version FROM database_version WHERE id = 1";

    // v1 update query
    private static final String ALTER_LOCKED_COLUMN_QUERY_MYSQL = "ALTER TABLE worlds MODIFY locked BIGINT NOT NULL DEFAULT 0";
    private static final String ALTER_LOCKED_COLUMN_QUERY_POSTGRES = "ALTER TABLE worlds ALTER COLUMN locked TYPE BIGINT";

    // World handling queries
    private static final String CREATE_WORLDS_TABLE_QUERY_MYSQL = "CREATE TABLE IF NOT EXISTS worlds (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) UNIQUE, world LONGBLOB, locked BIGINT NOT NULL DEFAULT 0)";
    private static final String CREATE_WORLDS_TABLE_QUERY_POSTGRES = "CREATE TABLE IF NOT EXISTS worlds (" +
            "id SERIAL PRIMARY KEY, name VARCHAR(255) UNIQUE, world BYTEA, locked BIGINT NOT NULL DEFAULT 0)";
    private static final String SELECT_WORLD_QUERY = "SELECT world, locked FROM worlds WHERE name = ?";
    private static final String UPDATE_WORLD_QUERY_MYSQL = "INSERT INTO worlds (name, world, locked) VALUES (?, ?, 1) " +
            "ON DUPLICATE KEY UPDATE world = VALUES(world)";
    private static final String UPDATE_WORLD_QUERY_POSTGRES = "INSERT INTO worlds (name, world, locked) VALUES (?, ?, 1) " +
            "ON CONFLICT (name) DO UPDATE SET world = EXCLUDED.world";
    private static final String UPDATE_LOCK_QUERY = "UPDATE worlds SET locked = ? WHERE name = ?";
    private static final String DELETE_WORLD_QUERY = "DELETE FROM worlds WHERE name = ?";
    private static final String LIST_WORLDS_QUERY = "SELECT name FROM worlds";

    private final Map<String, ScheduledFuture> lockedWorlds = new HashMap<>();
    private final HikariDataSource source;
    private final DatabaseType dbType;
    private final String loaderName;

    public enum DatabaseType {
        MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://", 3306),
        MARIADB("MariaDB", "org.mariadb.jdbc.Driver", "jdbc:mariadb://", 3306),
        POSTGRESQL("PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql://", 5432);

        private final String displayName;
        private final String driverClass;
        private final String jdbcPrefix;
        private final int defaultPort;

        DatabaseType(String displayName, String driverClass, String jdbcPrefix, int defaultPort) {
            this.displayName = displayName;
            this.driverClass = driverClass;
            this.jdbcPrefix = jdbcPrefix;
            this.defaultPort = defaultPort;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDriverClass() {
            return driverClass;
        }

        public String getJdbcPrefix() {
            return jdbcPrefix;
        }

        public int getDefaultPort() {
            return defaultPort;
        }
    }

    public SqlLoader(DatasourcesConfig.SqlConfig config, DatabaseType dbType, String loaderName) throws SQLException {
        this.dbType = dbType;
        this.loaderName = loaderName;
        
        // Load driver
        try {
            Class.forName(dbType.getDriverClass());
        } catch (ClassNotFoundException e) {
            throw new SQLException(dbType.getDisplayName() + " JDBC Driver not found", e);
        }
        
        HikariConfig hikariConfig = new HikariConfig();

        // Build JDBC URL
        String jdbcUrl = dbType.getJdbcPrefix() + config.getHost() + ":" + config.getPort() + "/" + config.getDatabase();
        
        // Add MySQL/MariaDB specific parameters
        if (dbType == DatabaseType.MYSQL || dbType == DatabaseType.MARIADB) {
            jdbcUrl += "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }
        
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(dbType.getDriverClass());

        // Pool settings - configurable
        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
        hikariConfig.setMinimumIdle(config.getMinIdle());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setIdleTimeout(config.getIdleTimeout());
        hikariConfig.setMaxLifetime(config.getMaxLifetime());
        hikariConfig.setLeakDetectionThreshold(60000); // 1 minute

        // Database specific optimizations
        if (dbType == DatabaseType.MYSQL || dbType == DatabaseType.MARIADB) {
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
            hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
            hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
            hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
        } else if (dbType == DatabaseType.POSTGRESQL) {
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            hikariConfig.addDataSourceProperty("reWriteBatchedInserts", "true");
            hikariConfig.addDataSourceProperty("socketTimeout", "30");
            hikariConfig.addDataSourceProperty("tcpKeepAlive", "true");
        }
        
        hikariConfig.addDataSourceProperty("ApplicationName", "SlimeWorldManager");

        source = new HikariDataSource(hikariConfig);

        try (Connection con = source.getConnection()) {
            // Create worlds table
            String createTableQuery = (dbType == DatabaseType.POSTGRESQL) ? 
                    CREATE_WORLDS_TABLE_QUERY_POSTGRES : CREATE_WORLDS_TABLE_QUERY_MYSQL;
            try (PreparedStatement statement = con.prepareStatement(createTableQuery)) {
                statement.execute();
            }

            // Create versioning table
            try (PreparedStatement statement = con.prepareStatement(CREATE_VERSIONING_TABLE_QUERY)) {
                statement.execute();
            }
            
            // Create indexes for better performance
            try (PreparedStatement statement = con.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_worlds_name ON worlds(name)")) {
                statement.execute();
            }
            
            try (PreparedStatement statement = con.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_worlds_locked ON worlds(locked)")) {
                statement.execute();
            }
            
            Logging.info(dbType.getDisplayName() + " loader initialized successfully");
        }
    }

    @Override
    public void update() throws IOException, NewerDatabaseException {
        try (Connection con = source.getConnection()) {
            int version;

            try (PreparedStatement statement = con.prepareStatement(GET_VERSION_QUERY);
                 ResultSet set = statement.executeQuery()) {
                version = set.next() ? set.getInt(1) : -1;
            }

            if (version > CURRENT_DB_VERSION) {
                throw new NewerDatabaseException(CURRENT_DB_VERSION, version);
            }

            if (version < CURRENT_DB_VERSION) {
                Logging.warning("Your SWM " + dbType.getDisplayName() + " database is outdated. The update process will start in 10 seconds.");
                Logging.warning("Note that this update might make your database incompatible with older SWM versions.");
                Logging.warning("Make sure no other servers with older SWM versions are using this database.");
                Logging.warning("Shut down the server to prevent your database from being updated.");

                try {
                    Thread.sleep(10000L);
                } catch (InterruptedException ignored) {

                }

                // Update to v1: alter locked column to store a long
                String alterQuery = (dbType == DatabaseType.POSTGRESQL) ? 
                        ALTER_LOCKED_COLUMN_QUERY_POSTGRES : ALTER_LOCKED_COLUMN_QUERY_MYSQL;
                try (PreparedStatement statement = con.prepareStatement(alterQuery)) {
                    statement.executeUpdate();
                }

                // Insert/update database version table
                String insertQuery = (dbType == DatabaseType.POSTGRESQL) ? 
                        INSERT_VERSION_QUERY_POSTGRES : INSERT_VERSION_QUERY_MYSQL;
                try (PreparedStatement statement = con.prepareStatement(insertQuery)) {
                    statement.setInt(1, CURRENT_DB_VERSION);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public byte[] loadWorld(String worldName, boolean readOnly) throws UnknownWorldException, IOException, WorldInUseException {
        Logging.info("Loading world: " + worldName + " (readOnly: " + readOnly + ") from " + dbType.getDisplayName());
        
        try (Connection con = source.getConnection();
            PreparedStatement statement = con.prepareStatement(SELECT_WORLD_QUERY)) {
            statement.setString(1, worldName);
            
            try (ResultSet set = statement.executeQuery()) {
                if (!set.next()) {
                    Logging.warning("World " + worldName + " not found in " + dbType.getDisplayName() + " database");
                    throw new UnknownWorldException(worldName);
                }

                if (!readOnly) {
                    long lockedMillis = set.getLong("locked");

                    if (System.currentTimeMillis() - lockedMillis <= LoaderUtils.MAX_LOCK_TIME) {
                        throw new WorldInUseException(worldName);
                    }

                    Logging.info("Acquiring lock for world: " + worldName);
                    updateLock(worldName, true);
                } else {
                    Logging.info("Loading world " + worldName + " in READ-ONLY mode");
                }

                byte[] worldData = set.getBytes("world");
                Logging.info("Successfully loaded world " + worldName + " (" + worldData.length + " bytes)");
                return worldData;
            }
        } catch (SQLException ex) {
            throw new IOException("Failed to load world " + worldName, ex);
        }
    }

    private void updateLock(String worldName, boolean forceSchedule) {
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(UPDATE_LOCK_QUERY)) {
            statement.setLong(1, System.currentTimeMillis());
            statement.setString(2, worldName);

            statement.executeUpdate();
        } catch (SQLException ex) {
            Logging.error("Failed to update the lock for world " + worldName + ":");
            ex.printStackTrace();
        }

        if (forceSchedule || lockedWorlds.containsKey(worldName)) {
            lockedWorlds.put(worldName, SERVICE.schedule(() -> updateLock(worldName, false), LoaderUtils.LOCK_INTERVAL, TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public boolean worldExists(String worldName) throws IOException {
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(SELECT_WORLD_QUERY)) {
            statement.setString(1, worldName);
            ResultSet set = statement.executeQuery();

            return set.next();
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public List<String> listWorlds() throws IOException {
        List<String> worldList = new ArrayList<>();

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(LIST_WORLDS_QUERY)) {
            ResultSet set = statement.executeQuery();

            while (set.next()) {
                worldList.add(set.getString("name"));
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }

        return worldList;
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld, boolean lock) throws IOException {
        String updateQuery = (dbType == DatabaseType.POSTGRESQL) ? 
                UPDATE_WORLD_QUERY_POSTGRES : UPDATE_WORLD_QUERY_MYSQL;
                
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(updateQuery)) {
            statement.setString(1, worldName);
            statement.setBytes(2, serializedWorld);
            statement.executeUpdate();

            if (lock) {
                updateLock(worldName, true);
            }
            
            Logging.info("World " + worldName + " saved successfully (" + serializedWorld.length + " bytes)");
        } catch (SQLException ex) {
            throw new IOException("Failed to save world " + worldName, ex);
        }
    }

    @Override
    public void unlockWorld(String worldName) throws IOException, UnknownWorldException {
        Logging.info("Unlocking world: " + worldName);
        
        ScheduledFuture future = lockedWorlds.remove(worldName);

        if (future != null) {
            future.cancel(false);
        }

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(UPDATE_LOCK_QUERY)) {
            statement.setLong(1, 0L);
            statement.setString(2, worldName);

            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated == 0) {
                throw new UnknownWorldException(worldName);
            }
            
            Logging.info("World " + worldName + " unlocked successfully");
        } catch (SQLException ex) {
            throw new IOException("Failed to unlock world " + worldName, ex);
        }
    }

    @Override
    public boolean isWorldLocked(String worldName) throws IOException, UnknownWorldException {
        if (lockedWorlds.containsKey(worldName)) {
            return true;
        }

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(SELECT_WORLD_QUERY)) {
            statement.setString(1, worldName);
            ResultSet set = statement.executeQuery();

            if (!set.next()) {
                throw new UnknownWorldException(worldName);
            }

            return System.currentTimeMillis() - set.getLong("locked") <= LoaderUtils.MAX_LOCK_TIME;
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void deleteWorld(String worldName) throws IOException, UnknownWorldException {
        Logging.warning("Deleting world: " + worldName);
        
        ScheduledFuture future = lockedWorlds.remove(worldName);

        if (future != null) {
            future.cancel(false);
        }

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(DELETE_WORLD_QUERY)) {
            statement.setString(1, worldName);

            if (statement.executeUpdate() == 0) {
                throw new UnknownWorldException(worldName);
            }
            
            Logging.warning("World " + worldName + " deleted successfully");
        } catch (SQLException ex) {
            throw new IOException("Failed to delete world " + worldName, ex);
        }
    }
    
    /**
     * Closes the HikariCP connection pool.
     * Should be called when the plugin is disabled.
     */
    public void close() {
        if (source != null && !source.isClosed()) {
            Logging.info("Closing " + dbType.getDisplayName() + " connection pool...");
            
            // Cancel all scheduled lock updates
            for (ScheduledFuture future : lockedWorlds.values()) {
                future.cancel(false);
            }
            lockedWorlds.clear();
            
            source.close();
            Logging.info(dbType.getDisplayName() + " connection pool closed");
        }
    }
}
