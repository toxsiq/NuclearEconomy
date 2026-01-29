package com.nucleareconomy;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NuclearEconomyPlugin extends JavaPlugin implements Listener {
    private Database database;
    private BalanceStore balanceStore;
    private ChequeService chequeService;
    private TopCache topCache;
    private EconomyAPI economyAPI;
    private final Map<UUID, PendingCheque> pendingCheques = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdirs()) {
                getLogger().warning("Nao foi possivel criar a pasta de dados.");
            }
        }
        database = new Database(new File(getDataFolder(), "economy.db"));
        try {
            database.connect();
        } catch (SQLException e) {
            getLogger().severe("Falha ao conectar no banco: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        balanceStore = new BalanceStore(this, database);
        chequeService = new ChequeService(this, balanceStore, database);
        topCache = new TopCache(this, database);
        economyAPI = new EconomyAPI(balanceStore, chequeService);
        topCache.start();

        for (Player player : Bukkit.getOnlinePlayers()) {
            balanceStore.loadPlayer(player);
        }

        registerEconomyCommands();
        registerAdminCommands();
        registerChequeCommands();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new ChequeListener(this, balanceStore, chequeService), this);
    }

    @Override
    public void onDisable() {
        if (balanceStore != null) {
            balanceStore.saveAllSync();
        }
        if (database != null) {
            database.close();
        }
    }

    private void registerEconomyCommands() {
        for (EconomyType type : EconomyType.values()) {
            PluginCommand command = getCommand(type.getCommand());
            if (command != null) {
                command.setExecutor(new EconomyCommand(this, type, balanceStore, database, topCache));
            }
        }
    }

    private void registerAdminCommands() {
        for (EconomyType type : EconomyType.values()) {
            PluginCommand command = getCommand("a" + type.getCommand());
            if (command != null) {
                command.setExecutor(new AdminEconomyCommand(type, balanceStore));
            }
        }
    }

    private void registerChequeCommands() {
        PluginCommand cheque = getCommand("cheque");
        if (cheque != null) {
            cheque.setExecutor(new ChequeCommand());
        }
        PluginCommand adminCheque = getCommand("acheque");
        if (adminCheque != null) {
            adminCheque.setExecutor(new AdminChequeCommand(this, chequeService));
        }
    }

    public void addPendingCheque(UUID uuid, PendingCheque pendingCheque) {
        pendingCheques.put(uuid, pendingCheque);
    }

    public PendingCheque removePendingCheque(UUID uuid) {
        return pendingCheques.remove(uuid);
    }

    public EconomyAPI getEconomyAPI() {
        return economyAPI;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        balanceStore.loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        balanceStore.unloadPlayer(event.getPlayer());
    }
}
