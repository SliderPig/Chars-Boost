package com.example.charboost;

import com.example.charboost.model.TagCensus;
import com.example.charboost.model.Tuner;

/** Shared state, read by the hooks every tick. Everything here belongs to the server thread. */
public final class Boost {
    public static final String MODID = "charboost";

    /** Master switch. */
    public static boolean enabled = true;
    /** 0 = healthy. Set by the tuner, or by hand with /charboost level. */
    public static int level = 0;
    /** -1 = automatic, otherwise pinned by /charboost level. */
    public static int pinnedLevel = -1;
    /** Server ticks since start. */
    public static long tick = 0;

    public static double mspt = 0;
    public static double tps = 20;

    public static final TagCensus CENSUS = new TagCensus();
    public static Tuner tuner = new Tuner(45, 10, 5, 20);

    private Boost() {
    }
}
