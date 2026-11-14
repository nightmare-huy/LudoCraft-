package ru.ludocraft.gameplay;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;

public final class GamblingRulesTest {
    public static void main(String[] args) {
        LudkaRulesTest.run();
        LudkaModValuesTest.run();
        everyLosingRollUsesOnlyOneRandomDraw();
        exhaustivelyChecksAllWinningRollsAndMultipliers();
        fallbackMatchesOreWordsButNotUnrelatedBlocks();
        System.out.println("PASS: 85 loss rolls, 60 jackpot combinations, ore-name regression cases");
    }
    private static void everyLosingRollUsesOnlyOneRandomDraw() {
        for (int roll = 1; roll <= 85; roll++) {
            int rawRoll = roll - 1;
            AtomicInteger calls = new AtomicInteger();
            assertEquals(0, GamblingRules.rollMultiplier(bound -> {
                assertEquals(100, bound);
                assertEquals(1, calls.incrementAndGet());
                return rawRoll;
            }));
            assertEquals(1, calls.get());
        }
    }

    private static void exhaustivelyChecksAllWinningRollsAndMultipliers() {
        int[] counts = new int[6];
        for (int roll = 86; roll <= 100; roll++) {
            for (int multiplier = 2; multiplier <= 5; multiplier++) {
                int rawRoll = roll - 1;
                int rawMultiplier = multiplier - 2;
                AtomicInteger calls = new AtomicInteger();
                int result = GamblingRules.rollMultiplier(bound -> {
                    int call = calls.incrementAndGet();
                    assertTrue(call <= 2, "unexpected RNG call");
                    assertEquals(call == 1 ? 100 : 4, bound);
                    return call == 1 ? rawRoll : rawMultiplier;
                });
                assertEquals(multiplier, result);
                assertEquals(2, calls.get());
                counts[result]++;
            }
        }
        assertArrayEquals(new int[]{0, 0, 15, 15, 15, 15}, counts);
    }

    private static void fallbackMatchesOreWordsButNotUnrelatedBlocks() {
        for (String name : new String[]{"diamond_ore", "deepslate_iron_ore", "nether_quartz_ore",
                "ore_tin", "ores/tin", "ore"}) {
            assertTrue(GamblingRules.hasOreWord(name), name);
        }
        for (String name : new String[]{"stone", "spore_blossom", "heavy_core", "oregon_log",
                "forest_leaves", "ancient_debris"}) {
            assertFalse(GamblingRules.hasOreWord(name), name);
        }
        // Ancient debris is recognized by c:ores in OreGambling, not by this name fallback.
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void assertFalse(boolean value, String message) {
        assertTrue(!value, message);
    }

    private static void assertArrayEquals(int[] expected, int[] actual) {
        assertTrue(Arrays.equals(expected, actual), Arrays.toString(actual));
    }
}
