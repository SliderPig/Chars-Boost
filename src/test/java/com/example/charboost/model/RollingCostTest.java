package com.example.charboost.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RollingCostTest {
    @Test
    void averagesAndTotals() {
        RollingCost c = new RollingCost();
        c.add(100);
        c.add(300);
        assertEquals(200, c.average(), 0.001);
        assertEquals(400, c.total());
        assertEquals(2, c.calls());
    }

    @Test
    void followsChanges() {
        RollingCost c = new RollingCost();
        for (int i = 0; i < 1000; i++) {
            c.add(100);
        }
        for (int i = 0; i < 2000; i++) {
            c.add(1000);
        }
        assertEquals(1000, c.average(), 10);
    }

    @Test
    void resets() {
        RollingCost c = new RollingCost();
        c.add(5);
        c.reset();
        assertEquals(0, c.total());
        assertEquals(0, c.average(), 0);
    }
}
