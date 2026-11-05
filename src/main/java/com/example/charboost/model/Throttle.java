package com.example.charboost.model;

/**
 * Picks which ticks to run when something runs "one tick in N".
 *
 * A plain "tick % N == 0" would be a disaster for scripts: a lot of them start with
 * "if (player.age % 4 != 0) return" and would simply never run on the ticks we keep. Stepping round the
 * golden ratio spreads the kept ticks evenly over every such pattern, so every "age % M" still gets its
 * fair share, about one in N of its turns.
 */
public final class Throttle {
    private static final long GOLDEN = 0x9E3779B97F4A7C15L;

    private Throttle() {
    }

    public static boolean keep(long tick, int salt, int oneIn) {
        if (oneIn <= 1) {
            return true;
        }
        // Top 32 bits of a walk round the circle, scaled to 0..oneIn-1.
        long position = (tick + salt * 0x61C88647L) * GOLDEN >>> 32;
        return (position * oneIn) >>> 32 == 0;
    }
}
