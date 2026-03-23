package com.grinderwolf.swm.plugin.config;

import lombok.Getter;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@Getter
@ConfigSerializable
public class DatasourcesConfig {

    @Setting("file") private FileConfig fileConfig = new FileConfig();
    @Setting("mysql") private SqlConfig mysqlConfig = new SqlConfig("root", "database", 3306);
    @Setting("mariadb") private SqlConfig mariadbConfig = new SqlConfig("root", "database", 3306);
    @Setting("postgresql") private SqlConfig postgresConfig = new SqlConfig("postgres", "postgres", 5432);

    @Getter
    @ConfigSerializable
    public static class SqlConfig {

        @Setting("enabled") private boolean enabled = false;

        @Setting("host") private String host = "localhost";
        @Setting("port") private int port;

        @Setting("username") private String username;
        @Setting("password") private String password = "";

        @Setting("database") private String database;
        
        // Connection pool settings
        @Setting("max-pool-size") private int maxPoolSize = 10;
        @Setting("min-idle") private int minIdle = 2;
        @Setting("connection-timeout") private int connectionTimeout = 30000;
        @Setting("idle-timeout") private int idleTimeout = 600000;
        @Setting("max-lifetime") private int maxLifetime = 1800000;

        // Default constructor for deserialization
        public SqlConfig() {
            this("root", "database", 3306);
        }

        // Constructor with custom defaults
        public SqlConfig(String username, String database, int port) {
            this.username = username;
            this.database = database;
            this.port = port;
        }
    }

    @Getter
    @ConfigSerializable
    public static class FileConfig {

        @Setting("path") private String path = "slime_worlds";

    }
}
