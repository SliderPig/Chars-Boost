package com.example.charboost.hook;

/** What every mob remembers for the AI throttle. Added to Mob by a mixin. */
public interface MobState {
    int charboost$interval();

    void charboost$interval(int interval);

    long charboost$nextLook();

    void charboost$nextLook(long tick);

    /** When each kind of work last ran, see the slots in {@link MobAi}. */
    long[] charboost$last();
}
