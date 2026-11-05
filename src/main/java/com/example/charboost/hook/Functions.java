package com.example.charboost.hook;

import com.example.charboost.Boost;
import com.example.charboost.Cfg;
import com.example.charboost.Opt;
import com.example.charboost.model.Rates;
import com.example.charboost.model.RollingCost;
import com.example.charboost.model.Throttle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandFunction;
import net.minecraft.resources.ResourceLocation;

/** Times datapack functions run from the tick tag or a schedule, and runs slow-listed tick ones less often. */
public final class Functions {
    private static final Map<ResourceLocation, Entry> ALL = new HashMap<>();
    private static long startTick = 0;
    private static long openedAt = 0;
    private static Entry opened = null;
    private static int depth = 0;

    public static final class Entry {
        public final String name;
        public final RollingCost cost = new RollingCost();
        public final int salt;
        public final boolean slow;
        public long skips;

        Entry(String name) {
            this.name = name;
            this.salt = name.hashCode();
            boolean found = false;
            for (String part : Cfg.slowFunctions) {
                if (name.contains(part)) {
                    found = true;
                    break;
                }
            }
            this.slow = found;
        }
    }

    private Functions() {
    }

    private static Entry entry(CommandFunction function) {
        return ALL.computeIfAbsent(function.getId(), id -> new Entry(id.toString()));
    }

    /** Called for each function of the tick tag: false = skip it this tick. */
    public static boolean tickDue(CommandFunction function) {
        Opt.SLOW_FUNCTIONS.seen++;
        Entry e = entry(function);
        if (!e.slow || !Opt.SLOW_FUNCTIONS.live() || Throttle.keep(Boost.tick, e.salt, Rates.slowScriptOneIn(Boost.level))) {
            return true;
        }
        e.skips++;
        Opt.SLOW_FUNCTIONS.skips++;
        Opt.SLOW_FUNCTIONS.savedNs += (long) e.cost.average();
        return false;
    }

    /** A call from inside another function only queues up, so only the outermost one is timed. */
    public static void open(CommandFunction function) {
        if (depth++ == 0) {
            opened = entry(function);
            openedAt = System.nanoTime();
        }
    }

    public static void close() {
        if (--depth <= 0) {
            depth = 0;
            if (opened != null) {
                opened.cost.add(System.nanoTime() - openedAt);
                opened = null;
            }
        }
    }

    /** Every tick, in case a function blew up and never closed. */
    public static void tickStart() {
        depth = 0;
        opened = null;
    }

    public static List<Entry> top() {
        List<Entry> list = new ArrayList<>(ALL.values());
        list.sort(Comparator.comparingLong((Entry e) -> e.cost.total()).reversed());
        return list;
    }

    public static long ticksCounted() {
        return Math.max(1, Boost.tick - startTick);
    }

    public static void reset() {
        for (Entry e : ALL.values()) {
            e.cost.reset();
            e.skips = 0;
        }
        startTick = Boost.tick;
    }
}
