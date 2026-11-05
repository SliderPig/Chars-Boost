package com.example.charboost.model;

/**
 * Decides how hard to throttle. Fed one reading a second (average tick time that second).
 * Level 0 = healthy, nothing is throttled, the game plays exactly like normal. Levels go up one at a
 * time while ticks stay over the target and come back down once they stay clearly under it.
 * If it keeps going down and straight back up, it waits longer before trying to go down again,
 * so the server doesn't wobble between the two.
 */
public final class Tuner {
    public static final int MAX_LEVEL = 3;

    private final double target;
    private final double margin;
    private final int raiseAfter;
    private final int lowerAfter;
    private final int maxLowerWait;

    private int level;
    private int overFor;
    private int underFor;
    private int lowerWait;
    /** Seconds since the level last went down, used to spot wobbling. */
    private int sinceLower = Integer.MAX_VALUE / 2;
    private int calmFor;

    public Tuner(double targetMs, double marginMs, int raiseAfterSeconds, int lowerAfterSeconds) {
        this.target = targetMs;
        this.margin = marginMs;
        this.raiseAfter = Math.max(1, raiseAfterSeconds);
        this.lowerAfter = Math.max(1, lowerAfterSeconds);
        this.lowerWait = this.lowerAfter;
        this.maxLowerWait = this.lowerAfter * 16;
    }

    public int level() {
        return level;
    }

    /** How long (seconds) ticks must stay healthy before the next step down. */
    public int lowerWait() {
        return lowerWait;
    }

    public void reset() {
        level = 0;
        overFor = 0;
        underFor = 0;
        lowerWait = lowerAfter;
        sinceLower = Integer.MAX_VALUE / 2;
        calmFor = 0;
    }

    /** One reading a second. Returns the level to use from now on. */
    public int feed(double msptThisSecond) {
        if (sinceLower < Integer.MAX_VALUE / 2) {
            sinceLower++;
        }
        if (msptThisSecond > target) {
            overFor++;
            underFor = 0;
            calmFor = 0;
            if (overFor >= raiseAfter && level < MAX_LEVEL) {
                level++;
                overFor = 0;
                // Went straight back up after stepping down: be slower to step down next time.
                if (sinceLower < lowerWait + raiseAfter * 2) {
                    lowerWait = Math.min(maxLowerWait, lowerWait * 2);
                }
            }
        } else if (msptThisSecond < target - margin) {
            underFor++;
            overFor = 0;
            calmFor++;
            if (underFor >= lowerWait && level > 0) {
                level--;
                underFor = 0;
                sinceLower = 0;
            }
            // A long calm spell forgives earlier wobbling.
            if (calmFor >= maxLowerWait && lowerWait > lowerAfter) {
                lowerWait = lowerAfter;
                calmFor = 0;
            }
        } else {
            // Between the two lines: hold where we are.
            overFor = 0;
            underFor = 0;
        }
        return level;
    }
}
