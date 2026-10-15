package com.pojavtiers.tagger.model;

public enum OverallTier {
    CADET('\uE010', 0xAEB6C4),
    ACE('\uE011', 0xE8517F),
    MASTER('\uE012', 0xF5B731),
    GRANDMASTER('\uE013', 0xFFD84D);

    public static final int ACE_MIN_POINTS = 50;
    public static final int MASTER_MIN_POINTS = 101;
    public static final int GRANDMASTER_MIN_POINTS = 150;

    private final char iconChar;
    private final int textColor;

    OverallTier(char iconChar, int textColor) {
        this.iconChar = iconChar;
        this.textColor = textColor;
    }

    public char iconChar() {
        return iconChar;
    }

    public int textColor() {
        return textColor;
    }

    public static OverallTier fromPoints(int points) {
        if (points >= GRANDMASTER_MIN_POINTS) return GRANDMASTER;
        if (points >= MASTER_MIN_POINTS) return MASTER;
        if (points >= ACE_MIN_POINTS) return ACE;
        return CADET;
    }
}
