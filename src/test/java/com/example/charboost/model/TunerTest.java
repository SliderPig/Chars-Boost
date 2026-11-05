package com.example.charboost.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TunerTest {
    private static int feed(Tuner t, double ms, int seconds) {
        int level = t.level();
        for (int i = 0; i < seconds; i++) {
            level = t.feed(ms);
        }
        return level;
    }

    @Test
    void healthyServerStaysAtZeroForever() {
        Tuner t = new Tuner(45, 10, 5, 20);
        assertEquals(0, feed(t, 20, 100000));
    }

    @Test
    void shortSpikeDoesNothing() {
        Tuner t = new Tuner(45, 10, 5, 20);
        feed(t, 200, 4);
        assertEquals(0, t.level());
        feed(t, 20, 1);
        feed(t, 200, 4);
        assertEquals(0, t.level());
    }

    @Test
    void climbsOneStepAtATimeUpToThree() {
        Tuner t = new Tuner(45, 10, 5, 20);
        assertEquals(1, feed(t, 80, 5));
        assertEquals(2, feed(t, 80, 5));
        assertEquals(3, feed(t, 80, 5));
        assertEquals(3, feed(t, 80, 500));
    }

    @Test
    void comesBackDownAndEndsAtVanilla() {
        Tuner t = new Tuner(45, 10, 5, 20);
        feed(t, 80, 15);
        assertEquals(3, t.level());
        assertEquals(2, feed(t, 20, 20));
        assertEquals(1, feed(t, 20, 20));
        assertEquals(0, feed(t, 20, 20));
    }

    @Test
    void holdsBetweenTheTwoLines() {
        Tuner t = new Tuner(45, 10, 5, 20);
        feed(t, 80, 5);
        assertEquals(1, t.level());
        assertEquals(1, feed(t, 40, 1000));
    }

    @Test
    void wobblingMakesItSlowerToStepDown() {
        Tuner t = new Tuner(45, 10, 5, 20);
        assertEquals(1, feed(t, 80, 5));
        int wait = t.lowerWait();
        assertEquals(0, feed(t, 20, 20));
        assertEquals(1, feed(t, 80, 5));
        assertTrue(t.lowerWait() > wait, "should wait longer after going straight back up");
        // A long calm spell forgives it.
        feed(t, 20, 2000);
        assertEquals(20, t.lowerWait());
    }

    @Test
    void waitNeverGrowsWithoutLimit() {
        Tuner t = new Tuner(45, 10, 5, 20);
        for (int i = 0; i < 50; i++) {
            feed(t, 80, 5);
            feed(t, 20, t.lowerWait());
        }
        assertTrue(t.lowerWait() <= 20 * 16);
    }
}
