package com.example.charboost.model;

/** How often a mob's AI runs: 1 = every tick. Further from the nearest player and harder pressure = less often. */
public final class AiRules {
    /** Distance limits in blocks. Under the first limit every tick, then 2, 4, 8 and 16 ticks. */
    private static final int[][] LIMITS = {
            {},                     // level 0: always every tick
            {32, 48, 64, 96},       // level 1
            {24, 32, 48, 64},       // level 2
            {16, 24, 32, 48},       // level 3
    };

    private AiRules() {
    }

    public static int interval(double distanceSquared, int level, int maxInterval) {
        if (level <= 0) {
            return 1;
        }
        int[] limits = LIMITS[Math.min(level, LIMITS.length - 1)];
        int interval = 1;
        for (int limit : limits) {
            if (distanceSquared < (double) limit * limit) {
                break;
            }
            interval *= 2;
        }
        return Math.min(interval, Math.max(1, maxInterval));
    }

    /**
     * For goals, which the game only looks at every 2 ticks: interval 2 means every 4 ticks, 4 every 8, and so on.
     * (The game calls this on every 2nd tick, so "2 x interval - 1" lands on the right call.)
     */
    public static boolean dueGoals(long now, long lastRun, int interval) {
        return interval <= 1 || now < lastRun || now - lastRun >= 2L * interval - 1;
    }

    /** For things the game does every tick. */
    public static boolean dueEvery(long now, long lastRun, int interval) {
        return interval <= 1 || now < lastRun || now - lastRun >= interval;
    }

    /** Under real pressure even near mobs (without a target) check for pushing less often. */
    public static int pushAtLeast(int level) {
        return level >= 2 ? 2 : 1;
    }

    /** How much of its normal search a far mob gets when looking for a path. */
    public static float pathDepth(int interval) {
        if (interval >= 8) {
            return 0.5f;
        }
        return interval >= 2 ? 0.75f : 1f;
    }

    /** How long a mob waits before trying a path again after the last try found no way there. */
    public static int pathRetryTicks(int level) {
        return switch (level) {
            case 0 -> 0;
            case 1 -> 10;
            case 2 -> 20;
            default -> 40;
        };
    }

    /** Despawn checks only run one tick in this many; the chance per check goes up to match. */
    public static int despawnEvery(int level) {
        return switch (level) {
            case 0 -> 1;
            case 1 -> 2;
            default -> 4;
        };
    }
}
