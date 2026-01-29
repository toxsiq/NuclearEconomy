package com.nucleareconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChequeService {
    private final NuclearEconomyPlugin plugin;
    private final BalanceStore balanceStore;
    private final Database database;
    private final NamespacedKey chequeIdKey;
    private final NamespacedKey chequeEconomyKey;
    private final NamespacedKey chequeValueKey;

    public ChequeService(NuclearEconomyPlugin plugin, BalanceStore balanceStore, Database database) {
        this.plugin = plugin;
        this.balanceStore = balanceStore;
        this.database = database;
        this.chequeIdKey = new NamespacedKey(plugin, "cheque-id");
        this.chequeEconomyKey = new NamespacedKey(plugin, "cheque-economy");
        this.chequeValueKey = new NamespacedKey(plugin, "cheque-value");
    }

    public ItemStack createCheque(EconomyType economy, double value, String creator) throws SQLException {
        UUID chequeId = UUID.randomUUID();
        database.insertCheque(new ChequeRecord(chequeId, economy, value, creator, false));

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + "Cheque de " + economy.getColor() + economy.getDisplayName() + ChatColor.GRAY + "!");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Criador: " + ChatColor.GRAY + creator);
            lore.add(ChatColor.WHITE + "Valor: " + economy.getColor() + economy.getSymbol() + " " + NumberFormatter.format(value));
            lore.add(ChatColor.DARK_GRAY + "ID: " + chequeId);
            meta.setLore(lore);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(chequeIdKey, PersistentDataType.STRING, chequeId.toString());
            container.set(chequeEconomyKey, PersistentDataType.STRING, economy.getCommand());
            container.set(chequeValueKey, PersistentDataType.DOUBLE, value);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void redeemCheque(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String idString = container.get(chequeIdKey, PersistentDataType.STRING);
        String economyKey = container.get(chequeEconomyKey, PersistentDataType.STRING);
        Double value = container.get(chequeValueKey, PersistentDataType.DOUBLE);
        if (idString == null || economyKey == null || value == null) {
            return;
        }
        EconomyType economy = EconomyType.fromCommand(economyKey);
        if (economy == null) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                UUID id = UUID.fromString(idString);
                ChequeRecord record = database.loadCheque(id);
                if (record == null || record.redeemed()) {
                    Bukkit.getScheduler().runTask(plugin, () -> MessageUtil.sendEconomyMessage(player, economy,
                            "-------------------------",
                            "CHEQUE",
                            "Este cheque ja foi usado.",
                            "-------------------------"));
                    return;
                }
                database.markChequeRedeemed(id);
                balanceStore.addBalance(player.getUniqueId(), player.getName(), economy, record.value());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    int amount = item.getAmount();
                    if (amount <= 1) {
                        player.getInventory().removeItem(item);
                    } else {
                        item.setAmount(amount - 1);
                    }
                    MessageUtil.sendEconomyMessage(player, economy,
                            "-------------------------",
                            "CHEQUE",
                            "Voce resgatou " + economy.getSymbol() + " " + NumberFormatter.format(record.value()) + " de " + economy.getDisplayName() + "!",
                            "-------------------------");
                });
            } catch (SQLException e) {
                plugin.getLogger().warning("Falha ao resgatar cheque: " + e.getMessage());
            }
        });
    }

    public double computeServerChequeValue(EconomyType economy) throws SQLException {
        double total = database.sumEconomy(economy);
        return total * 0.00005D;
    }
}
