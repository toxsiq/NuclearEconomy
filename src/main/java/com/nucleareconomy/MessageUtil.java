package com.nucleareconomy;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class MessageUtil {
    private MessageUtil() {
    }

    public static void sendEconomyMessage(CommandSender sender, EconomyType type, String... lines) {
        StringBuilder builder = new StringBuilder("\n");
        for (String line : lines) {
            builder.append(type.getColor()).append(line).append("\n");
        }
        builder.append("\n");
        sender.sendMessage(builder.toString());
    }

    public static void sendPlainMessage(CommandSender sender, ChatColor color, String... lines) {
        StringBuilder builder = new StringBuilder("\n");
        for (String line : lines) {
            builder.append(color).append(line).append("\n");
        }
        builder.append("\n");
        sender.sendMessage(builder.toString());
    }
}
