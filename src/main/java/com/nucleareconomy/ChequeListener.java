package com.nucleareconomy;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.Locale;

public class ChequeListener implements Listener {
    private final NuclearEconomyPlugin plugin;
    private final BalanceStore balanceStore;
    private final ChequeService chequeService;

    public ChequeListener(NuclearEconomyPlugin plugin, BalanceStore balanceStore, ChequeService chequeService) {
        this.plugin = plugin;
        this.balanceStore = balanceStore;
        this.chequeService = chequeService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryView view = event.getView();
        String title = view.getTitle();
        if (!title.equals(ChequeMenu.PLAYER_TITLE) && !title.equals(ChequeMenu.ADMIN_TITLE)) {
            return;
        }
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        EconomyType selected = null;
        for (EconomyType type : EconomyType.values()) {
            if (item.getType() == type.getMenuMaterial()) {
                selected = type;
                break;
            }
        }
        if (selected == null) {
            return;
        }
        boolean admin = title.equals(ChequeMenu.ADMIN_TITLE);
        plugin.addPendingCheque(player.getUniqueId(), new PendingCheque(selected, admin));
        player.closeInventory();
        MessageUtil.sendEconomyMessage(player, selected,
                "-------------------------",
                "&lCHEQUE",
                "Digite o valor do cheque no chat.",
                "-------------------------");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PendingCheque pending = plugin.removePendingCheque(player.getUniqueId());
        if (pending == null) {
            return;
        }
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        EconomyType economy = pending.economy();
        boolean admin = pending.admin();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                double value;
                if (admin && message.equalsIgnoreCase("servidor")) {
                    value = chequeService.computeServerChequeValue(economy);
                } else {
                    value = NumberFormatter.parse(message);
                }
                if (value <= 0) {
                    sendInvalidValue(player, economy);
                    return;
                }
                if (!admin) {
                    boolean removed = balanceStore.removeBalance(player.getUniqueId(), player.getName(), economy, value);
                    if (!removed) {
                        Bukkit.getScheduler().runTask(plugin, () -> MessageUtil.sendEconomyMessage(player, economy,
                                "-------------------------",
                                "&lCHEQUE",
                                "Saldo insuficiente.",
                                "-------------------------"));
                        return;
                    }
                }
                String creator = admin ? "Servidor" : player.getName();
                ItemStack cheque = chequeService.createCheque(economy, value, creator);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.getInventory().addItem(cheque);
                    MessageUtil.sendEconomyMessage(player, economy,
                            "-------------------------",
                            "&lCHEQUE",
                            "Cheque criado com sucesso!",
                            "-------------------------");
                });
            } catch (IllegalArgumentException ex) {
                sendInvalidValue(player, economy);
            } catch (SQLException e) {
                plugin.getLogger().warning("Falha ao criar cheque: " + e.getMessage());
            }
        });
    }

    private void sendInvalidValue(Player player, EconomyType economy) {
        Bukkit.getScheduler().runTask(plugin, () -> MessageUtil.sendEconomyMessage(player, economy,
                "-------------------------",
                "&lCHEQUE",
                "Valor invalido.",
                "-------------------------"));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        if (!item.getItemMeta().getDisplayName().toLowerCase(Locale.ROOT).contains("cheque")) {
            return;
        }
        event.setCancelled(true);
        chequeService.redeemCheque(event.getPlayer(), item);
    }
}
