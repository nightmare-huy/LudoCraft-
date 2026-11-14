package ru.ludocraft.gameplay;

import java.util.function.IntUnaryOperator;

/** Pure rules, kept separate from Minecraft so the exact boundaries can be unit-tested. */
public final class GamblingRules {
    private GamblingRules() {}

    /** @param nextInt uniform bounded RNG: same contract as RandomSource.nextInt(bound).
     *  @return zero for a loss, otherwise the final drop multiplier (2, 3, 4 or 5).
     */
    public static int rollMultiplier(IntUnaryOperator nextInt) {
        int roll = nextInt.applyAsInt(100) + 1;
        return roll <= 85 ? 0 : nextInt.applyAsInt(4) + 2;
    }

    /** Match registry path words, not substrings such as spore_blossom or heavy_core. */
    public static boolean hasOreWord(String path) {
        for (String word : path.split("[_/.-]")) {
            if (word.equals("ore") || word.equals("ores")) {
                return true;
            }
        }
        return false;
    }
}
