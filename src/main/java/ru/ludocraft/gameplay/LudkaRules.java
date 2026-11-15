package ru.ludocraft.gameplay;

import java.util.Locale;

/** Values are virtual balancing units, not money or Minecraft's name-color rarity. */
public final class LudkaRules {
    public static final int SPIN_TICKS = 60;
    public static final long SPIN_NANOS = 3_000_000_000L;
    public static final int PRECISION = 1_000_000;
    public static final int MAX_VALUE = 32767;

    private LudkaRules() {}

    public static int chance(int stakeValue, int targetValue) {
        if (stakeValue <= 0 || targetValue <= 0) return 0;
        long chance = (long) PRECISION * Math.min(stakeValue, MAX_VALUE) / (2L * Math.min(targetValue, MAX_VALUE));
        return (int) Math.clamp(chance, 1, 950_000);
    }
    public static boolean wins(int chance, int roll) {
        return chance > 0 && chance <= 950_000 && roll >= 0 && roll < PRECISION && roll < chance;
    }
    public static String percent(int chance) {
        return String.format(Locale.ROOT, "%.4f%%", chance * 100.0 / PRECISION);
    }
    public static double runner(double progress, double percent) {
        double t = Math.clamp(progress, 0.0, 1.0);
        double travel = (600.0 + percent) * (1.0 - Math.pow(1.0 - t, 3));
        double phase = travel % 200.0;
        return phase <= 100.0 ? phase : 200.0 - phase;
    }
    public static int rarity(int value) {
        if (value >= 4096) return 4;
        if (value >= 512) return 3;
        if (value >= 64) return 2;
        if (value >= 8) return 1;
        return 0;
    }
}
