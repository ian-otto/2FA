package com.lielamar.auth.database.mysql;

import com.lielamar.auth.Main;
import com.lielamar.auth.database.AuthenticationDatabase;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.*;
import java.util.UUID;

public class AuthMySQL implements AuthenticationDatabase {

    private final Main main;

    private Connection connection;
    private String host, database, username, password;
    private int port;

    public AuthMySQL(Main main) {
        this.main = main;
    }


    /**
     * Sets up the database from the Config.yml file
     *
     * @return   Whether or not connection was successful
     */
    public boolean setup() {
        if(this.main.getConfig() == null)
            this.main.saveConfig();

        if(!this.main.getConfig().getBoolean("MySQL.enabled")) return false;

        ConfigurationSection mysqlSection = this.main.getConfig().getConfigurationSection("MySQL");
        this.host = mysqlSection.getString("credentials.host");
        this.database = mysqlSection.getString("credentials.database");
        this.port = mysqlSection.getInt("credentials.port");
        this.username = mysqlSection.getString("credentials.auth.username");
        this.password = mysqlSection.getString("credentials.auth.password");

        try {
            openConnection();
            return true;
        } catch(SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            this.connection = null;
            return false;
        }
    }

    /**
     * Opens a MySQL connection
     *
     * @throws SQLException             Throws an exception if couldn't connect to the sql database
     * @throws ClassNotFoundException   Throws an exception if the Driver class wasn't found
     */
    private void openConnection() throws SQLException, ClassNotFoundException {
        if(isValidConnection())
            throw new IllegalStateException("A MySQL instance already exists for the following database: " + this.database);

        synchronized(this) {
            if(isValidConnection()) throw new IllegalStateException("A MySQL instance already exists for the following database: " + this.database);

            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s", this.host, this.port, this.database), this.username, this.password);

            createTables();
        }
    }

    /**
     * Creates the required tables on the database
     *
     * @throws SQLException   Throws an exception if couldn't connect to the sql database
     */
    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS Auth(uuid varchar(64), secret_key varchar(64), last_ip varchar(64));";
        PreparedStatement stmt = this.connection.prepareStatement(sql);
        stmt.executeUpdate();
    }

    /**
     * Checks if the connection is valid (not null & open)
     *
     * @return                Whether or not the connection is valid
     * @throws SQLException   Throws an exception if the connection is not available
     */
    public boolean isValidConnection() throws SQLException {
        return this.connection != null && !this.connection.isClosed();
    }


    @Override
    public String setSecretKey(UUID uuid, String secretKey) {
        try {
            if(!isValidConnection()) return null;

            PreparedStatement statement;
            statement = this.connection.prepareStatement("SELECT * FROM Auth WHERE uuid = ?;");
            ResultSet result = statement.executeQuery();

            if(result.next()) {
                statement = this.connection.prepareStatement("UPDATE Auth SET secret_key = ? WHERE uuid = ?;");
                statement.setString(1, secretKey);
                statement.setString(2, uuid.toString());
            } else {
                statement = this.connection.prepareStatement("INSERT INTO Auth(uuid, secret_key, last_ip) VALUES (?,?);");
                statement.setString(1, uuid.toString());
                statement.setString(2, secretKey);
                statement.setString(3, "");
            }
            statement.executeUpdate();

            AuthenticationDatabase.cachedKeys.put(uuid, secretKey);
            return secretKey;
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getSecretKey(UUID uuid) {
        if(AuthenticationDatabase.cachedKeys.containsKey(uuid))
            return AuthenticationDatabase.cachedKeys.get(uuid);

        try {
            if(!isValidConnection()) return null;

            PreparedStatement statement = this.connection.prepareStatement("SELECT secret_key FROM Auth WHERE uuid = ?;");
            statement.setString(1, uuid.toString());
            ResultSet result = statement.executeQuery();

            if(result.next()) {
                String secretKey = result.getString("secret_key");
                AuthenticationDatabase.cachedKeys.put(uuid, secretKey);
                return secretKey.equalsIgnoreCase("") ? null : secretKey;
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean hasSecretKey(UUID uuid) {
        return this.getSecretKey(uuid) != null;
    }

    @Override
    public void removeSecretKey(UUID uuid) {
        this.setSecretKey(uuid, "");
    }


    @Override
    public String setLastIP(UUID uuid, String lastIP) {
        try {
            if(!isValidConnection()) return null;

            PreparedStatement statement;
            statement = this.connection.prepareStatement("SELECT * FROM Auth WHERE uuid = ?;");
            ResultSet result = statement.executeQuery();

            if(result.next()) {
                statement = this.connection.prepareStatement("UPDATE Auth SET last_ip = ? WHERE uuid = ?;");
                statement.setString(1, lastIP);
                statement.setString(2, uuid.toString());
            } else {
                statement = this.connection.prepareStatement("INSERT INTO Auth(uuid, secret_key, last_ip) VALUES (?,?);");
                statement.setString(1, uuid.toString());
                statement.setString(2, "");
                statement.setString(3, lastIP);
            }
            statement.executeUpdate();
            return lastIP;
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getLastIP(UUID uuid) {
        try {
            if(!isValidConnection()) return null;

            PreparedStatement statement = this.connection.prepareStatement("SELECT last_ip FROM Auth WHERE uuid = ?;");
            statement.setString(1, uuid.toString());
            ResultSet result = statement.executeQuery();

            if(result.next()) {
                String lastIP = result.getString("last_ip");
                return lastIP.equalsIgnoreCase("") ? null : lastIP;
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean hasLastIP(UUID uuid) {
        return this.getLastIP(uuid) != null;
    }

    @Override
    public void removeLastIP(UUID uuid) {
        this.setLastIP(uuid, "");
    }
}