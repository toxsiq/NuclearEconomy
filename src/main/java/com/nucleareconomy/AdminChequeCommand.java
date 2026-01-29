package com.nucleareconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;

public class AdminChequeCommand implements CommandExecutor {
    private final NuclearEconomyPlugin plugin;
    private final ChequeService chequeService;

    public AdminChequeCommand(NuclearEconomyPlugin plugin, ChequeService chequeService) {
        this.plugin = plugin;
        this.chequeService = chequeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nucleareconomy.admin")) {
            sender.sendMessage(ChatColor.RED + "Sem permissao.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores podem usar esse comando.");
            return true;
        }
        if (args.length == 0) {
            player.openInventory(ChequeMenu.createAdminMenu());
            return true;
        }
        if (args.length >= 3 && args[0].equalsIgnoreCase("criar")) {
            EconomyType economy = EconomyType.fromCommand(args[1]);
            if (economy == null) {
                sender.sendMessage(ChatColor.RED + "Economia invalida.");
                return true;
            }
            String valueText = args[2];
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    double value = valueText.equalsIgnoreCase("servidor")
                            ? chequeService.computeServerChequeValue(economy)
                            : NumberFormatter.parse(valueText);
                    if (value <= 0) {
                        sender.sendMessage(ChatColor.RED + "Valor invalido.");
                        return;
                    }
                    ItemStack cheque = chequeService.createCheque(economy, value, "Servidor");
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.getInventory().addItem(cheque);
                        MessageUtil.sendEconomyMessage(sender, economy,
                                "-------------------------",
                                "CHEQUE ADMIN",
                                "Cheque criado com sucesso!",
                                "-------------------------");
                    });
                } catch (SQLException | IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Nao foi possivel criar o cheque.");
                }
            });
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Use: /acheque [criar <economia> <valor>] ");
        return true;
    }
}
