package com.example.charboost;

import java.util.Locale;

/**
 * One optimisation with its switch and its score. Two kinds:
 * always-on ones change nothing about how the game plays, they just stop wasted work;
 * load ones change timing a little, so they only wake up while the tuner says the server is struggling.
 */
public enum Opt {
    SELECTOR_INDEX("selectorIndex", false, "@e[type=x] looks only at that type, not every entity"),
    SELECTOR_TAGS("selectorTags", false, "@e[tag=x] finds nothing at once if nobody has the tag"),
    POWER_PROVIDERS("powerProviders", false, "Palladium skips its armour, suit and power search while nothing changed"),
    ABILITY_CONTEXT("abilityContext", false, "each Palladium ability keeps its condition data instead of rebuilding it every tick"),
    PROPERTIES("palladiumProperties", false, "property lookups by name are instant, and new entities copy their property list instead of running every script"),
    SAVES("asyncSaves", false, "player files, advancements, stats and world data are written on another core"),
    MOB_AI("mobAi", true, "mobs far from players think less often"),
    MOB_PUSH("mobPush", true, "far mobs check for pushing less often"),
    PATHS("pathLimits", true, "far mobs search shorter paths, stuck mobs wait before searching again"),
    HOPPERS("hoppers", true, "idle hoppers rest a few ticks between looks"),
    DESPAWN("despawnChecks", true, "despawn checks run less often, with a matching higher chance"),
    AFK("afkPlayers", true, "scripts and powers of AFK players run less often"),
    SLOW_SCRIPTS("slowScripts", true, "KubeJS tick scripts on the slow list run less often"),
    SLOW_FUNCTIONS("slowFunctions", true, "datapack tick functions on the slow list run less often");

    public final String key;
    /** True = only runs while the server is under load. */
    public final boolean loadOnly;
    public final String about;

    public boolean on = true;
    /** Times it stepped in. */
    public long skips;
    /** Estimated time saved, in nanoseconds. */
    public long savedNs;
    /** Times the code it hooks has run: if this stays at 0 the hook isn't in the game. */
    public long seen;

    long windowStart;
    public double savedMsPerTick;

    Opt(String key, boolean loadOnly, String about) {
        this.key = key;
        this.loadOnly = loadOnly;
        this.about = about;
    }

    /** Is it doing anything right now? */
    public boolean live() {
        return on && Boost.enabled && (!loadOnly || Boost.level > 0);
    }

    public static Opt byKey(String key) {
        for (Opt o : values()) {
            if (o.key.toLowerCase(Locale.ROOT).equals(key.toLowerCase(Locale.ROOT))) {
                return o;
            }
        }
        return null;
    }
}
