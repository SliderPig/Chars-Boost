package com.example.charboost.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AiRulesTest {
    private static double sq(double blocks) {
        return blocks * blocks;
    }

    @Test
    void levelZeroIsAlwaysEveryTick() {
        assertEquals(1, AiRules.interval(sq(1000), 0, 16));
        assertEquals(1, AiRules.interval(Double.MAX_VALUE, 0, 16));
    }

    @Test
    void furtherAwayMeansLessOften() {
        assertEquals(1, AiRules.interval(sq(10), 1, 16));
        assertEquals(1, AiRules.interval(sq(31), 1, 16));
        assertEquals(2, AiRules.interval(sq(32), 1, 16));
        assertEquals(4, AiRules.interval(sq(50), 1, 16));
        assertEquals(8, AiRules.interval(sq(70), 1, 16));
        assertEquals(16, AiRules.interval(sq(100), 1, 16));
    }

    @Test
    void morePressureReachesCloser() {
        for (double d = 0; d < 120; d += 3) {
            assertTrue(AiRules.interval(sq(d), 2, 16) >= AiRules.interval(sq(d), 1, 16));
            assertTrue(AiRules.interval(sq(d), 3, 16) >= AiRules.interval(sq(d), 2, 16));
        }
        assertEquals(1, AiRules.interval(sq(15), 3, 16));
    }

    @Test
    void noPlayersAtAllIsTheFarthestTier() {
        assertEquals(16, AiRules.interval(Double.MAX_VALUE, 1, 16));
    }

    @Test
    void maxIntervalIsRespected() {
        assertEquals(4, AiRules.interval(sq(500), 3, 4));
        assertEquals(1, AiRules.interval(sq(500), 3, 1));
    }

    @Test
    void goalsAtIntervalFourRunEveryEightTicks() {
        // The game only gets to the full goal tick on every 2nd tick.
        long last = 0;
        int runs = 0;
        for (long now = 2; now <= 80; now += 2) {
            if (AiRules.dueGoals(now, last, 4)) {
                last = now;
                runs++;
            }
        }
        assertEquals(10, runs);
    }

    @Test
    void goalsAtIntervalOneAlwaysRun() {
        assertTrue(AiRules.dueGoals(5, 5, 1));
    }

    @Test
    void everyTickThingsRunAtTheirInterval() {
        long last = 0;
        int runs = 0;
        for (long now = 1; now <= 80; now++) {
            if (AiRules.dueEvery(now, last, 4)) {
                last = now;
                runs++;
            }
        }
        assertEquals(20, runs);
    }

    @Test
    void timeGoingBackwardsNeverStopsAMob() {
        assertTrue(AiRules.dueEvery(10, 5000, 16));
        assertTrue(AiRules.dueGoals(10, 5000, 16));
        assertFalse(AiRules.dueEvery(5001, 5000, 16));
    }

    @Test
    void nearMobsGetFullPathSearches() {
        assertEquals(1f, AiRules.pathDepth(1));
        assertTrue(AiRules.pathDepth(16) < AiRules.pathDepth(2));
        assertEquals(0, AiRules.pathRetryTicks(0));
    }

    @Test
    void despawnRateStaysTheSame() {
        for (int level = 0; level <= 3; level++) {
            int every = AiRules.despawnEvery(level);
            // Asked every "every" ticks with 1 in (800/every): still about 1 in 800 per tick.
            assertEquals(800, every * (800 / every));
        }
    }
}
