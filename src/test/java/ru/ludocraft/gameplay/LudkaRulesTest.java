package ru.ludocraft.gameplay;

public final class LudkaRulesTest {
    public static void run() {
        int[] expected = {50, 25, 10, 5};
        for (int risk = 0; risk < 4; risk++) {
            int wins = 0;
            for (int roll = 0; roll < 100; roll++) {
                if (LudkaRules.wins(risk, roll)) wins++;
                check(Math.abs(LudkaRules.runner(1, roll) - roll) < 1e-9, "Wrong final position");
                check(LudkaRules.runner(0, roll) == 0, "Wrong starting position");
                for (int frame = 0; frame <= 180; frame++) {
                    double position = LudkaRules.runner(frame / 180.0, roll);
                    check(position >= 0 && position <= 100, "Runner outside bar");
                }
            }
            check(wins == expected[risk], "Incorrect probability");
            check(!LudkaRules.wins(risk, -1) && !LudkaRules.wins(risk, 100), "Invalid result accepted");
            check(LudkaRules.wins(risk, expected[risk] - 1), "Last green position lost");
            check(!LudkaRules.wins(risk, expected[risk]), "First red position won");
        }
        check(!LudkaRules.validRisk(-1) && !LudkaRules.validRisk(4), "Invalid button accepted");
        check(LudkaRules.SPIN_NANOS == 3_000_000_000L && LudkaRules.SPIN_TICKS == 60, "Wrong duration");
        System.out.println("PASS: Ludka odds, boundaries, 100 endpoints and 72,400 animation samples");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
