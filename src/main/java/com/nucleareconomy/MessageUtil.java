package com.nucleareconomy;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class MessageUtil {
    private MessageUtil() {
    }

    public static void sendEconomyMessage(CommandSender sender, EconomyType type, String... lines) {
        StringBuilder builder = new StringBuilder("\n");
        for (String line : lines) {
            String resolved = line;
            if (shouldPrefixColor(line)) {
                resolved = type.getColor() + line;
            }
            builder.append(ChatColor.translateAlternateColorCodes('&', resolved)).append("\n");
        }
        builder.append("\n");
        sender.sendMessage(builder.toString());
    }

    public static void sendPlainMessage(CommandSender sender, ChatColor color, String... lines) {
        StringBuilder builder = new StringBuilder("\n");
        for (String line : lines) {
            String resolved = line;
            if (shouldPrefixColor(line)) {
                resolved = color + line;
            }
            builder.append(ChatColor.translateAlternateColorCodes('&', resolved)).append("\n");
        }
        builder.append("\n");
        sender.sendMessage(builder.toString());
    }

    private static boolean shouldPrefixColor(String line) {
        if (line.startsWith("&") || line.startsWith("§")) {
            if (line.length() < 2) {
                return true;
            }
            char code = Character.toLowerCase(line.charAt(1));
            return "klmno r".indexOf(code) >= 0;
        }
        return true;
    }
}
