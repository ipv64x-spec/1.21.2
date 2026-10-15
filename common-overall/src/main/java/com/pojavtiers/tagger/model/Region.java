package com.pojavtiers.tagger.model;

import java.util.Locale;

public final class Region {
    private Region() {}

    public static String displayCode(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty() || s.equalsIgnoreCase("unknown")) return null;
        return s.toUpperCase(Locale.ROOT);
    }

    public static String colorTierCode(String displayCode) {
        if (displayCode == null) return null;
        return switch (displayCode.toUpperCase(Locale.ROOT)) {
            case "AS" -> "LT3";
            case "NA" -> "HT3";
            case "EU" -> "HT4";
            default -> null;
        };
    }
}
