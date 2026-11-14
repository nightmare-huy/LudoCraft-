package ru.ludocraft.gameplay;

import java.util.function.IntUnaryOperator;

/** Pure, deterministic odds for the data-driven Gambling enchantment. */
public final class EnchantedGamblingRules {
    private static final int[][] MULTIPLIERS = {
            {},
            {2, 3, 4, 5},
            {3, 4, 5, 6, 7, 8},
            {5, 8, 10, 12, 15}
    };
    private static final int[] LOSS_PERCENT = {0, 85, 75, 65};

    private EnchantedGamblingRules() {}

    /** Returns zero for a loss, otherwise one multiplier selected uniformly for the level. */
    public static int rollMultiplier(int level, IntUnaryOperator nextInt) {
        if (level < 1 || level >= MULTIPLIERS.length) return 0;
        if (nextInt.applyAsInt(100) < LOSS_PERCENT[level]) return 0;
        int[] values = MULTIPLIERS[level];
        return values[nextInt.applyAsInt(values.length)];
    }

    public static int lossPercent(int level) {
        return level >= 1 && level < LOSS_PERCENT.length ? LOSS_PERCENT[level] : 100;
    }

    public static int[] multipliers(int level) {
        return level >= 1 && level < MULTIPLIERS.length ? MULTIPLIERS[level].clone() : new int[0];
    }
}
