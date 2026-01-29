package com.nucleareconomy;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ChequeMenu {
    public static final String PLAYER_TITLE = ChatColor.GRAY + "Cheques";
    public static final String ADMIN_TITLE = ChatColor.GRAY + "Cheques Admin";

    private ChequeMenu() {
    }

    public static Inventory createPlayerMenu() {
        Inventory inventory = org.bukkit.Bukkit.createInventory(null, 27, PLAYER_TITLE);
        inventory.setItem(11, createItem(EconomyType.MONEY));
        inventory.setItem(15, createItem(EconomyType.CASH));
        return inventory;
    }

    public static Inventory createAdminMenu() {
        Inventory inventory = org.bukkit.Bukkit.createInventory(null, 27, ADMIN_TITLE);
        int slot = 10;
        for (EconomyType type : EconomyType.values()) {
            inventory.setItem(slot, createItem(type));
            slot++;
            if ((slot + 1) % 9 == 0) {
                slot += 2;
            }
        }
        return inventory;
    }

    private static ItemStack createItem(EconomyType economy) {
        ItemStack item = new ItemStack(economy.getMenuMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(economy.getColor() + economy.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Clique para criar cheque.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
