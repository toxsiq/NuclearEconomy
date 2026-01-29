package com.nucleareconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdminEconomyCommand implements CommandExecutor {
    private final EconomyType economy;
    private final BalanceStore balanceStore;

    public AdminEconomyCommand(EconomyType economy, BalanceStore balanceStore) {
        this.economy = economy;
        this.balanceStore = balanceStore;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nucleareconomy.admin")) {
            sender.sendMessage(ChatColor.RED + "Sem permissao.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Use: /a" + economy.getCommand() + " <set|add|remove> <jogador> <valor>");
            return true;
        }
        String action = args[0].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        double amount;
        try {
            amount = NumberFormatter.parse(args[2]);
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + "Valor invalido.");
            return true;
        }
        if (amount < 0) {
            sender.sendMessage(ChatColor.RED + "Valor invalido.");
            return true;
        }
        String name = target.getName() == null ? args[1] : target.getName();
        switch (action) {
            case "set" -> balanceStore.setBalance(target.getUniqueId(), name, economy, amount);
            case "add" -> balanceStore.addBalance(target.getUniqueId(), name, economy, amount);
            case "remove" -> balanceStore.removeBalance(target.getUniqueId(), name, economy, amount);
            default -> {
                sender.sendMessage(ChatColor.RED + "Use: /a" + economy.getCommand() + " <set|add|remove> <jogador> <valor>");
                return true;
            }
        }
        MessageUtil.sendEconomyMessage(sender, economy,
                "-------------------------",
                "ADMIN",
                "Voce ajustou " + economy.getSymbol() + " " + NumberFormatter.format(amount) + " de " + economy.getDisplayName() + " para " + name + "!",
                "-------------------------");
        return true;
    }
}
