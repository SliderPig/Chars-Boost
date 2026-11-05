package com.example.charboost.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RatesTest {
    @Test
    void healthyMeansNoHopperRest() {
        assertEquals(0, Rates.hopperRest(0, false, 8));
        assertEquals(0, Rates.hopperRest(0, true, 8));
    }

    @Test
    void hoppersNearPlayersRestLess() {
        for (int level = 1; level <= 3; level++) {
            assertTrue(Rates.hopperRest(level, true, 8) <= Rates.hopperRest(level, false, 8));
        }
        assertEquals(0, Rates.hopperRest(1, true, 8));
    }

    @Test
    void neverLongerThanAHoppersOwnPause() {
        for (int level = 0; level <= 5; level++) {
            for (int max = -1; max <= 100; max++) {
                assertTrue(Rates.hopperRest(level, false, max) <= 8);
                assertTrue(Rates.hopperRest(level, false, max) >= 0);
            }
        }
    }

    @Test
    void afkAndSlowScriptsAreNormalWhenHealthy() {
        assertEquals(1, Rates.afkOneIn(0));
        assertEquals(1, Rates.slowScriptOneIn(0));
    }

    @Test
    void moreLoadSlowsMore() {
        for (int level = 0; level < 3; level++) {
            assertTrue(Rates.afkOneIn(level + 1) >= Rates.afkOneIn(level));
            assertTrue(Rates.slowScriptOneIn(level + 1) >= Rates.slowScriptOneIn(level));
        }
    }
}
