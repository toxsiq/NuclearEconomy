package com.nucleareconomy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BalanceStore {
    private final NuclearEconomyPlugin plugin;
    private final Database database;
    private final Map<UUID, Map<EconomyType, Double>> cache = new ConcurrentHashMap<>();

    public BalanceStore(NuclearEconomyPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void loadPlayer(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Map<EconomyType, Double> balances = database.loadBalances(player.getUniqueId(), player.getName());
                cache.put(player.getUniqueId(), balances);
            } catch (SQLException e) {
                plugin.getLogger().warning("Falha ao carregar saldo de " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    public void unloadPlayer(Player player) {
        savePlayer(player.getUniqueId(), player.getName());
        cache.remove(player.getUniqueId());
    }

    public double getBalance(UUID uuid, EconomyType type) {
        Map<EconomyType, Double> balances = cache.get(uuid);
        if (balances == null) {
            return 0D;
        }
        return balances.getOrDefault(type, 0D);
    }

    public void setBalance(UUID uuid, String name, EconomyType type, double amount) {
        Map<EconomyType, Double> balances = cache.computeIfAbsent(uuid, key -> new EnumMap<>(EconomyType.class));
        balances.put(type, Math.max(0D, amount));
        savePlayer(uuid, name);
    }

    public void addBalance(UUID uuid, String name, EconomyType type, double amount) {
        Map<EconomyType, Double> balances = cache.computeIfAbsent(uuid, key -> new EnumMap<>(EconomyType.class));
        double current = balances.getOrDefault(type, 0D);
        balances.put(type, Math.max(0D, current + amount));
        savePlayer(uuid, name);
    }

    public boolean removeBalance(UUID uuid, String name, EconomyType type, double amount) {
        Map<EconomyType, Double> balances = cache.computeIfAbsent(uuid, key -> new EnumMap<>(EconomyType.class));
        double current = balances.getOrDefault(type, 0D);
        if (current < amount) {
            return false;
        }
        balances.put(type, Math.max(0D, current - amount));
        savePlayer(uuid, name);
        return true;
    }

    public void saveAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayer(player.getUniqueId(), player.getName());
        }
    }

    private void savePlayer(UUID uuid, String name) {
        Map<EconomyType, Double> balances = cache.get(uuid);
        if (balances == null) {
            return;
        }
        if (!plugin.isEnabled()) {
            savePlayerSync(uuid, name, balances);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                database.saveBalances(uuid, name, balances);
            } catch (SQLException e) {
                plugin.getLogger().warning("Falha ao salvar saldo de " + name + ": " + e.getMessage());
            }
        });
    }

    public void saveAllSync() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Map<EconomyType, Double> balances = cache.get(uuid);
            if (balances != null) {
                savePlayerSync(uuid, player.getName(), balances);
            }
        }
    }

    private void savePlayerSync(UUID uuid, String name, Map<EconomyType, Double> balances) {
        try {
            database.saveBalances(uuid, name, balances);
        } catch (SQLException e) {
            plugin.getLogger().warning("Falha ao salvar saldo de " + name + ": " + e.getMessage());
        }
    }
}
