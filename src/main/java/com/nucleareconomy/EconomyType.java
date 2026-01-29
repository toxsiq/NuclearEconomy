package com.nucleareconomy;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum EconomyType {
    MONEY("money", "Money", ChatColor.GREEN, "$", Material.EMERALD, true),
    TOKENS("tokens", "Tokens", ChatColor.YELLOW, "✦", Material.NETHER_STAR, false),
    CASH("cash", "Cash", ChatColor.GOLD, "✪", Material.GOLD_INGOT, true),
    TOXINA("toxina", "Toxina", ChatColor.DARK_GREEN, "☠", Material.SLIME_BALL, false),
    RADIACAO("radiacao", "Radiação", ChatColor.RED, "☢", Material.FERMENTED_SPIDER_EYE, false),
    PEIXES("peixes", "Peixes", ChatColor.DARK_AQUA, "⚓", Material.COD, false),
    REAIS("reais", "Reais", ChatColor.AQUA, "R$", Material.DIAMOND, false);

    private final String command;
    private final String displayName;
    private final ChatColor color;
    private final String symbol;
    private final Material menuMaterial;
    private final boolean supportsPay;

    EconomyType(String command, String displayName, ChatColor color, String symbol, Material menuMaterial, boolean supportsPay) {
        this.command = command;
        this.displayName = displayName;
        this.color = color;
        this.symbol = symbol;
        this.menuMaterial = menuMaterial;
        this.supportsPay = supportsPay;
    }

    public String getCommand() {
        return command;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getSymbol() {
        return symbol;
    }

    public Material getMenuMaterial() {
        return menuMaterial;
    }

    public boolean supportsPay() {
        return supportsPay;
    }

    public static EconomyType fromCommand(String command) {
        for (EconomyType type : values()) {
            if (type.command.equalsIgnoreCase(command)) {
                return type;
            }
        }
        return null;
    }
}
