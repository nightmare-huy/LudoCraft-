package ru.ludocraft.gameplay;

/** Shared arithmetic only; no client classes and no client-side random outcomes. */
public final class LudkaRules {
    public static final int SPIN_TICKS = 60;
    public static final long SPIN_NANOS = 3_000_000_000L;
    private static final int[] CHANCES = {50, 25, 10, 5};
    private static final int[] MULTIPLIERS = {2, 4, 10, 20};

    private LudkaRules() {}

    public static boolean validRisk(int risk) { return risk >= 0 && risk < CHANCES.length; }
    public static int chance(int risk) {
        if (!validRisk(risk)) throw new IllegalArgumentException("Invalid risk: " + risk);
        return CHANCES[risk];
    }
    public static int multiplier(int risk) {
        if (!validRisk(risk)) throw new IllegalArgumentException("Invalid risk: " + risk);
        return MULTIPLIERS[risk];
    }
    public static boolean wins(int risk, int roll) {
        // 100 equally likely positions, not 101: exact 50/25/10/5 percent probabilities.
        return roll >= 0 && roll < 100 && roll < chance(risk);
    }
    public static double runner(double progress, int roll) {
        double t = Math.clamp(progress, 0.0, 1.0);
        double travel = (600.0 + roll) * (1.0 - Math.pow(1.0 - t, 3));
        double phase = travel % 200.0;
        return phase <= 100.0 ? phase : 200.0 - phase;
    }
}
