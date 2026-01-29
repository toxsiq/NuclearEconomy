package com.nucleareconomy;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Database {
    private final File file;
    private Connection connection;

    public Database(File file) {
        this.file = file;
    }

    public void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS balances (" +
                    "uuid TEXT PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "money REAL NOT NULL DEFAULT 0," +
                    "tokens REAL NOT NULL DEFAULT 0," +
                    "cash REAL NOT NULL DEFAULT 0," +
                    "toxina REAL NOT NULL DEFAULT 0," +
                    "radiacao REAL NOT NULL DEFAULT 0," +
                    "peixes REAL NOT NULL DEFAULT 0," +
                    "reais REAL NOT NULL DEFAULT 0" +
                    ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS cheques (" +
                    "id TEXT PRIMARY KEY," +
                    "economy TEXT NOT NULL," +
                    "value REAL NOT NULL," +
                    "creator TEXT NOT NULL," +
                    "redeemed INTEGER NOT NULL DEFAULT 0" +
                    ")");
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // ignored
            }
        }
    }

    public synchronized Map<EconomyType, Double> loadBalances(UUID uuid, String name) throws SQLException {
        Map<EconomyType, Double> balances = new EnumMap<>(EconomyType.class);
        for (EconomyType type : EconomyType.values()) {
            balances.put(type, 0D);
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM balances WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    for (EconomyType type : EconomyType.values()) {
                        balances.put(type, resultSet.getDouble(type.getCommand()));
                    }
                    updateName(uuid, name);
                    return balances;
                }
            }
        }
        insertPlayer(uuid, name);
        return balances;
    }

    private void insertPlayer(UUID uuid, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO balances (uuid, name) VALUES (?, ?)")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.executeUpdate();
        }
    }

    private void updateName(UUID uuid, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE balances SET name = ? WHERE uuid = ?")) {
            statement.setString(1, name);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        }
    }

    public synchronized void saveBalances(UUID uuid, String name, Map<EconomyType, Double> balances) throws SQLException {
        StringBuilder builder = new StringBuilder("UPDATE balances SET name = ?");
        for (EconomyType type : EconomyType.values()) {
            builder.append(", ").append(type.getCommand()).append(" = ?");
        }
        builder.append(" WHERE uuid = ?");
        try (PreparedStatement statement = connection.prepareStatement(builder.toString())) {
            statement.setString(1, name);
            int index = 2;
            for (EconomyType type : EconomyType.values()) {
                statement.setDouble(index++, balances.getOrDefault(type, 0D));
            }
            statement.setString(index, uuid.toString());
            statement.executeUpdate();
        }
    }

    public synchronized LinkedHashMap<String, Double> loadTop(EconomyType type, int limit) throws SQLException {
        LinkedHashMap<String, Double> top = new LinkedHashMap<>();
        String sql = "SELECT name, " + type.getCommand() + " as value FROM balances ORDER BY value DESC LIMIT ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    top.put(resultSet.getString("name"), resultSet.getDouble("value"));
                }
            }
        }
        return top;
    }

    public synchronized double sumEconomy(EconomyType type) throws SQLException {
        String sql = "SELECT SUM(" + type.getCommand() + ") as total FROM balances";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getDouble("total");
            }
        }
        return 0D;
    }

    public synchronized void insertCheque(ChequeRecord cheque) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO cheques (id, economy, value, creator, redeemed) VALUES (?, ?, ?, ?, 0)")) {
            statement.setString(1, cheque.id().toString());
            statement.setString(2, cheque.economy().getCommand());
            statement.setDouble(3, cheque.value());
            statement.setString(4, cheque.creator());
            statement.executeUpdate();
        }
    }

    public synchronized ChequeRecord loadCheque(UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM cheques WHERE id = ?")) {
            statement.setString(1, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    EconomyType type = EconomyType.fromCommand(resultSet.getString("economy"));
                    return new ChequeRecord(id, type, resultSet.getDouble("value"),
                            resultSet.getString("creator"), resultSet.getInt("redeemed") == 1);
                }
            }
        }
        return null;
    }

    public synchronized void markChequeRedeemed(UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE cheques SET redeemed = 1 WHERE id = ?")) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        }
    }
}
