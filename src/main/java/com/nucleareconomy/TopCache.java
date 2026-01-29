package com.nucleareconomy;

import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class TopCache {
    private final NuclearEconomyPlugin plugin;
    private final Database database;
    private final Map<EconomyType, LinkedHashMap<String, Double>> topData = new EnumMap<>(EconomyType.class);
    private Instant nextUpdate = Instant.now();

    public TopCache(NuclearEconomyPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        for (EconomyType type : EconomyType.values()) {
            topData.put(type, new LinkedHashMap<>());
        }
    }

    public void start() {
        update();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::update, 20L * 300, 20L * 300);
    }

    public Duration timeUntilUpdate() {
        return Duration.between(Instant.now(), nextUpdate).isNegative() ? Duration.ZERO : Duration.between(Instant.now(), nextUpdate);
    }

    public LinkedHashMap<String, Double> getTop(EconomyType type) {
        return topData.getOrDefault(type, new LinkedHashMap<>());
    }

    private void update() {
        for (EconomyType type : EconomyType.values()) {
            try {
                topData.put(type, database.loadTop(type, 10));
            } catch (SQLException e) {
                plugin.getLogger().warning("Falha ao atualizar top de " + type.getDisplayName() + ": " + e.getMessage());
            }
        }
        nextUpdate = Instant.now().plusSeconds(300);
    }
}
