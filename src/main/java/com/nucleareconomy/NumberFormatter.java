package com.nucleareconomy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class NumberFormatter {
    private static final Map<String, BigDecimal> SUFFIXES = new LinkedHashMap<>();

    static {
        SUFFIXES.put("D", new BigDecimal("1e33"));
        SUFFIXES.put("N", new BigDecimal("1e30"));
        SUFFIXES.put("OC", new BigDecimal("1e27"));
        SUFFIXES.put("SS", new BigDecimal("1e24"));
        SUFFIXES.put("S", new BigDecimal("1e21"));
        SUFFIXES.put("QQ", new BigDecimal("1e18"));
        SUFFIXES.put("Q", new BigDecimal("1e15"));
        SUFFIXES.put("T", new BigDecimal("1e12"));
        SUFFIXES.put("B", new BigDecimal("1e9"));
        SUFFIXES.put("M", new BigDecimal("1e6"));
        SUFFIXES.put("K", new BigDecimal("1e3"));
    }

    private NumberFormatter() {
    }

    public static String format(double value) {
        BigDecimal amount = BigDecimal.valueOf(value).setScale(4, RoundingMode.DOWN).stripTrailingZeros();
        BigDecimal abs = amount.abs();
        for (Map.Entry<String, BigDecimal> entry : SUFFIXES.entrySet()) {
            if (abs.compareTo(entry.getValue()) >= 0) {
                BigDecimal scaled = amount.divide(entry.getValue(), 2, RoundingMode.DOWN).stripTrailingZeros();
                return scaled.toPlainString() + entry.getKey();
            }
        }
        return amount.setScale(2, RoundingMode.DOWN).stripTrailingZeros().toPlainString();
    }

    public static double parse(String input) {
        String normalized = input.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Valor inválido");
        }
        for (Map.Entry<String, BigDecimal> entry : SUFFIXES.entrySet()) {
            if (normalized.endsWith(entry.getKey())) {
                String number = normalized.substring(0, normalized.length() - entry.getKey().length());
                return new BigDecimal(number)
                        .multiply(entry.getValue())
                        .setScale(4, RoundingMode.DOWN)
                        .doubleValue();
            }
        }
        return new BigDecimal(normalized).setScale(4, RoundingMode.DOWN).doubleValue();
    }
}
