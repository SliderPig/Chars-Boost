package com.example.charboost.hook;

import com.example.charboost.Boost;
import com.example.charboost.Cfg;
import com.example.charboost.Opt;
import com.example.charboost.model.Rates;
import com.example.charboost.model.RollingCost;
import com.example.charboost.model.Throttle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Times every KubeJS event handler by script and line, which spark can't do, and runs slow-listed tick ones less often. */
public final class Scripts {
    private static final ConcurrentHashMap<String, Entry> ALL = new ConcurrentHashMap<>();
    private static long startTick = 0;
    private static List<String> slowSeen = null;

    public static final class Entry {
        public final String name;
        public final RollingCost cost = new RollingCost();
        public final int salt;
        public boolean tickEvent;
        public boolean slow;
        public long skips;
        /** Java that does the same job, if this is one of the known worst scripts. */
        public Consumer<Object> replacement;
        public long replacementSaves;

        Entry(String name) {
            this.name = name;
            this.salt = name.hashCode();
        }
    }

    private Scripts() {
    }

    public static Entry entry(String source, int line) {
        String name = source + ":" + line;
        return ALL.computeIfAbsent(name, n -> {
            Entry e = new Entry(n);
            e.slow = listed(n);
            if (PowersHook.hasPalladium()) {
                e.replacement = NativeScripts.find(n);
                e.replacementSaves = NativeScripts.estimate(n);
            }
            return e;
        });
    }

    private static boolean listed(String name) {
        for (String part : Cfg.slowScripts) {
            if (name.contains(part)) {
                return true;
            }
        }
        return false;
    }

    /** True if this handler should sit this one out. */
    public static boolean skip(Entry entry) {
        if (!entry.slow || !entry.tickEvent || !Opt.SLOW_SCRIPTS.live()) {
            return false;
        }
        if (Throttle.keep(Boost.tick, entry.salt, Rates.slowScriptOneIn(Boost.level))) {
            return false;
        }
        entry.skips++;
        Opt.SLOW_SCRIPTS.skips++;
        Opt.SLOW_SCRIPTS.savedNs += (long) entry.cost.average();
        return true;
    }

    /** Runs the script's handler or its Java replacement. */
    public static boolean useReplacement(Entry entry) {
        if (entry.replacement == null || !Opt.NATIVE_SCRIPTS.live()) {
            return false;
        }
        Opt.NATIVE_SCRIPTS.seen++;
        Opt.NATIVE_SCRIPTS.skips++;
        Opt.NATIVE_SCRIPTS.savedNs += entry.replacementSaves;
        return true;
    }

    /** Called when the config changes. */
    public static void refreshLists() {
        if (slowSeen != Cfg.slowScripts) {
            slowSeen = Cfg.slowScripts;
            for (Entry e : ALL.values()) {
                e.slow = listed(e.name);
            }
        }
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
