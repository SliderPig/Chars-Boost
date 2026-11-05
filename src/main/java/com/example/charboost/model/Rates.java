package com.example.charboost.model;

/** How much the load-sensitive parts slow down at each pressure level. 1 = every tick. */
public final class Rates {
    private Rates() {
    }

    /** Ticks an idle hopper rests before looking again. Never more than 8, a hopper's normal pause between items. */
    public static int hopperRest(int level, boolean playerNear, int maxRest) {
        int rest = switch (level) {
            case 0 -> 0;
            case 1 -> playerNear ? 0 : 4;
            case 2 -> playerNear ? 2 : 8;
            default -> playerNear ? 4 : 8;
        };
        return Math.min(rest, Math.min(8, Math.max(0, maxRest)));
    }

    /** An AFK player's tick scripts and powers run one tick in this many. */
    public static int afkOneIn(int level) {
        return switch (level) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 3;
            default -> 4;
        };
    }

    /** A slow-listed script runs one tick in this many. */
    public static int slowScriptOneIn(int level) {
        return switch (level) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 3;
            default -> 4;
        };
    }

    /** A locked ability looks at its unlock conditions one tick in this many. */
    public static int lockedAbilityEvery(int level, boolean afk) {
        if (afk) {
            return level == 0 ? 1 : 8;
        }
        return switch (level) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 3;
            default -> 4;
        };
    }
}
