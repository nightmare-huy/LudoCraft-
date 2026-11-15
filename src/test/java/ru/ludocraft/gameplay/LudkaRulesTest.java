package ru.ludocraft.gameplay;

public final class LudkaRulesTest {
    public static void run() {
        check(LudkaRules.chance(1, 2048) == 244, "Dirt -> netherite must be 0.0244%");
        check(LudkaRules.chance(128, 2048) == 31250, "Diamond -> netherite must be 3.125%");
        check(LudkaRules.chance(2048, 2048) == 500000, "Equal value must be 50%");
        check(LudkaRules.chance(2048, 1) == 950000, "Chance must be capped at 95%");
        check(LudkaRules.chance(0, 128) == 0 && LudkaRules.chance(128, 0) == 0, "Empty quote");
        check(LudkaRules.chance(1, LudkaRules.MAX_VALUE) > 0, "Any permitted target must be possible");
        for (int chance : new int[]{15, 244, 31250, 500000, 950000}) {
            int wins = 0;
            for (int roll = 0; roll < LudkaRules.PRECISION; roll++) if (LudkaRules.wins(chance, roll)) wins++;
            check(wins == chance, "Incorrect exact probability");
            check(!LudkaRules.wins(chance, -1) && !LudkaRules.wins(chance, LudkaRules.PRECISION), "Invalid roll accepted");
            check(LudkaRules.wins(chance, chance - 1) && !LudkaRules.wins(chance, chance), "Wrong green/red boundary");
        }
        int previous = 0;
        for (int value = 1; value <= LudkaRules.MAX_VALUE; value++) {
            int chance = LudkaRules.chance(value, 2048);
            check(chance >= previous, "Higher stake reduces chance");
            previous = chance;
            // No profitable low-end floor: expected output value is <= input value.
            check((long) LudkaRules.chance(1, value) * value <= LudkaRules.PRECISION, "Cheap stake exploits rounding");
        }
        for (double endpoint : new double[]{0, 0.0015, 0.0244, 3.125, 50, 95, 99.9999}) {
            check(Math.abs(LudkaRules.runner(1, endpoint) - endpoint) < 1e-8, "Wrong endpoint");
            for (int frame = 0; frame <= 180; frame++) {
                double pos = LudkaRules.runner(frame / 180.0, endpoint);
                check(pos >= 0 && pos <= 100, "Runner outside bar");
            }
        }
        check(LudkaRules.rarity(1) < LudkaRules.rarity(2048), "Dirt and netherite in same tier");
        check(LudkaRules.percent(244).equals("0.0244%"), "Tiny chance formatting");
        System.out.println("PASS: target valuation odds, 5 million outcomes, monotonicity, tiny odds and animation");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
