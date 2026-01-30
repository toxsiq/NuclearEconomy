package com.nucleareconomy;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.UUID;

public class EconomyAPI {
    private final BalanceStore balanceStore;
    private final ChequeService chequeService;

    public EconomyAPI(BalanceStore balanceStore, ChequeService chequeService) {
        this.balanceStore = balanceStore;
        this.chequeService = chequeService;
    }

    public double getBalance(UUID uuid, EconomyType economy) {
        return balanceStore.getBalance(uuid, economy);
    }

    public void setBalance(UUID uuid, String name, EconomyType economy, double value) {
        balanceStore.setBalance(uuid, name, economy, value);
    }

    public void addBalance(UUID uuid, String name, EconomyType economy, double value) {
        balanceStore.addBalance(uuid, name, economy, value);
    }

    public boolean removeBalance(UUID uuid, String name, EconomyType economy, double value) {
        return balanceStore.removeBalance(uuid, name, economy, value);
    }

    public ItemStack createCheque(EconomyType economy, double value, String creator) throws SQLException {
        return chequeService.createCheque(economy, value, creator);
    }

    public ItemStack createServerCheque(EconomyType economy) throws SQLException {
        double value = chequeService.computeServerChequeValue(economy);
        return chequeService.createCheque(economy, value, "Servidor");
    }

    public double computeServerValue(EconomyType economy) throws SQLException {
        return chequeService.computeServerChequeValue(economy);
    }

    public void redeemCheque(Player player, ItemStack cheque) {
        chequeService.redeemCheque(player, cheque);
    }
}
