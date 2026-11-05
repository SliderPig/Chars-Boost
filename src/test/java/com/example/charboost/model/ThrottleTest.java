package com.example.charboost.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThrottleTest {
    @Test
    void oneInOneKeepsEverything() {
        for (long t = 0; t < 1000; t++) {
            assertTrue(Throttle.keep(t, 5, 1));
        }
    }

    @Test
    void keepsAboutTheRightShare() {
        for (int oneIn : new int[]{2, 3, 4, 8}) {
            int kept = 0;
            int total = 100000;
            for (long t = 0; t < total; t++) {
                if (Throttle.keep(t, 12345, oneIn)) {
                    kept++;
                }
            }
            assertEquals(1.0 / oneIn, (double) kept / total, 0.01, "one in " + oneIn);
        }
    }

    /** The whole point: scripts written as "if (age % M != 0) return" must never be starved, whatever M is. */
    @Test
    void everyAgeModuloPatternGetsItsFairShare() {
        for (int oneIn : new int[]{2, 3, 4}) {
            for (int m = 2; m <= 40; m++) {
                for (int phase = 0; phase < m; phase++) {
                    int kept = 0;
                    int turns = 4000;
                    for (int k = 0; k < turns; k++) {
                        if (Throttle.keep(phase + (long) k * m, 777, oneIn)) {
                            kept++;
                        }
                    }
                    double share = (double) kept / turns;
                    assertEquals(1.0 / oneIn, share, 0.08, "m=" + m + " phase=" + phase + " oneIn=" + oneIn);
                }
            }
        }
    }

    @Test
    void neverAVeryLongGap() {
        int longest = 0;
        int gap = 0;
        for (long t = 0; t < 200000; t++) {
            if (Throttle.keep(t, 99, 2)) {
                gap = 0;
            } else {
                gap++;
                longest = Math.max(longest, gap);
            }
        }
        assertTrue(longest <= 4, "longest gap " + longest);
    }

    @Test
    void differentSaltsDontAllSkipTogether() {
        int same = 0;
        int total = 10000;
        for (long t = 0; t < total; t++) {
            if (Throttle.keep(t, 1, 2) == Throttle.keep(t, 2, 2)) {
                same++;
            }
        }
        assertTrue(same < total * 0.9, "two salts agree too often: " + same);
    }
}
