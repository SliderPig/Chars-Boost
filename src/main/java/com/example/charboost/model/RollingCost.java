package com.example.charboost.model;

/** A running average of how long something takes, plus the total. All in nanoseconds. */
public final class RollingCost {
    private double average;
    private long total;
    private long calls;

    public void add(long nanos) {
        total += nanos;
        calls++;
        // Quick to settle at first, then follows a moving average over the last 200 or so.
        double weight = calls < 200 ? 1.0 / calls : 1.0 / 200;
        average += (nanos - average) * weight;
    }

    public double average() {
        return average;
    }

    public long total() {
        return total;
    }

    public long calls() {
        return calls;
    }

    public void reset() {
        average = 0;
        total = 0;
        calls = 0;
    }
}
