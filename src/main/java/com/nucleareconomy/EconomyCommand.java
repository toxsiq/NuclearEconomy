package com.nucleareconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyCommand implements CommandExecutor {
    private final NuclearEconomyPlugin plugin;
    private final EconomyType economy;
    private final BalanceStore balanceStore;
    private final Database database;
    private final TopCache topCache;

    public EconomyCommand(NuclearEconomyPlugin plugin, EconomyType economy, BalanceStore balanceStore, Database database, TopCache topCache) {
        this.plugin = plugin;
        this.economy = economy;
        this.balanceStore = balanceStore;
        this.database = database;
        this.topCache = topCache;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores podem usar esse comando.");
            return true;
        }
        if (args.length == 0) {
            double balance = balanceStore.getBalance(player.getUniqueId(), economy);
            sendBalanceMessage(player, player.getName(), balance, true);
            return true;
        }
        if (args[0].equalsIgnoreCase("top")) {
            sendTop(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("pay") || args[0].equalsIgnoreCase("enviar")) {
            if (!economy.supportsPay()) {
                MessageUtil.sendEconomyMessage(player, economy,
                        "-------------------------",
                        "PAGAMENTOS",
                        "Essa economia nao suporta pagamentos.",
                        "-------------------------");
                return true;
            }
            if (args.length < 3) {
                MessageUtil.sendEconomyMessage(player, economy,
                        "-------------------------",
                        "PAGAMENTOS",
                        "Use: /" + economy.getCommand() + " pay <jogador> <valor>",
                        "-------------------------");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                MessageUtil.sendEconomyMessage(player, economy,
                        "-------------------------",
                        "PAGAMENTOS",
                        "Jogador nao encontrado.",
                        "-------------------------");
                return true;
            }
            double amount;
            try {
                amount = NumberFormatter.parse(args[2]);
            } catch (IllegalArgumentException ex) {
                MessageUtil.sendEconomyMessage(player, economy,
                        "-------------------------",
                        "PAGAMENTOS",
                        "Valor invalido.",
                        "-------------------------");
                return true;
            }
            if (amount <= 0) {
                MessageUtil.sendEconomyMessage(player, economy,
                        "-------------------------",
                        "PAGAMENTOS",
                        "O valor deve ser maior que zero.",
                        "-------------------------");
                return true;
            }
            if (!balanceStore.removeBalance(player.getUniqueId(), player.getName(), economy, amount)) {
                MessageUtil.sendEconomyMessage(player, economy,
                        "-------------------------",
                        "PAGAMENTOS",
                        "Saldo insuficiente.",
                        "-------------------------");
                return true;
            }
            balanceStore.addBalance(target.getUniqueId(), target.getName(), economy, amount);
            MessageUtil.sendEconomyMessage(player, economy,
                    "-------------------------",
                    "PAGAMENTOS",
                    "Voce enviou " + economy.getSymbol() + " " + NumberFormatter.format(amount) + " de " + economy.getDisplayName() + " para " + target.getName() + "!",
                    "-------------------------");
            MessageUtil.sendEconomyMessage(target, economy,
                    "-------------------------",
                    "PAGAMENTOS",
                    "Voce recebeu " + economy.getSymbol() + " " + NumberFormatter.format(amount) + " de " + economy.getDisplayName() + " de " + player.getName() + "!",
                    "-------------------------");
            return true;
        }
        if (args.length == 1) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            UUID targetId = target.getUniqueId();
            if (target.isOnline()) {
                double balance = balanceStore.getBalance(targetId, economy);
                sendBalanceMessage(player, target.getName(), balance, false);
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        Map<EconomyType, Double> balances = database.loadBalances(targetId, target.getName() == null ? args[0] : target.getName());
                        double balance = balances.getOrDefault(economy, 0D);
                        Bukkit.getScheduler().runTask(plugin, () -> sendBalanceMessage(player, target.getName() == null ? args[0] : target.getName(), balance, false));
                    } catch (SQLException e) {
                        plugin.getLogger().warning("Falha ao carregar saldo: " + e.getMessage());
                    }
                });
            }
            return true;
        }
        MessageUtil.sendPlainMessage(player, ChatColor.RED, "Uso incorreto do comando.");
        return true;
    }

    private void sendBalanceMessage(Player player, String targetName, double balance, boolean self) {
        String line = self
                ? "Voce possui " + economy.getSymbol() + " " + NumberFormatter.format(balance) + " de " + economy.getDisplayName()
                : "O jogador " + targetName + " possui " + economy.getSymbol() + " " + NumberFormatter.format(balance) + " de " + economy.getDisplayName();
        MessageUtil.sendEconomyMessage(player, economy,
                "-------------------------",
                "SEU SALDO",
                line,
                "-------------------------");
    }

    private void sendTop(Player player) {
        LinkedHashMap<String, Double> top = topCache.getTop(economy);
        Duration remaining = topCache.timeUntilUpdate();
        long minutes = remaining.toMinutes();
        long seconds = remaining.minusMinutes(minutes).toSeconds();
        MessageUtil.sendEconomyMessage(player, economy,
                "-------------------------",
                economy.getDisplayName().toUpperCase() + " TOP",
                "(Atualiza em " + String.format("%02d:%02d", minutes, seconds) + ")",
                buildTopLines(top),
                "-------------------------");
    }

    private String buildTopLines(LinkedHashMap<String, Double> top) {
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (Map.Entry<String, Double> entry : top.entrySet()) {
            builder.append(index).append("° ").append(entry.getKey())
                    .append(" - ").append(economy.getSymbol()).append(" ").append(NumberFormatter.format(entry.getValue())).append(" de ")
                    .append(economy.getDisplayName()).append("\n");
            index++;
        }
        if (builder.length() == 0) {
            return "Nenhum dado disponivel.";
        }
        return builder.toString().trim();
    }
}
