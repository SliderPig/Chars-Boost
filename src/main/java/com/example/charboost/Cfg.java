package com.example.charboost;

import com.example.charboost.model.Tuner;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;

/** config/charboost.toml. Read once at start and again whenever the file is changed. */
public final class Cfg {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.DoubleValue TARGET;
    private static final ForgeConfigSpec.DoubleValue MARGIN;
    private static final ForgeConfigSpec.IntValue RAISE;
    private static final ForgeConfigSpec.IntValue LOWER;
    private static final Map<Opt, ForgeConfigSpec.BooleanValue> SWITCHES = new EnumMap<>(Opt.class);
    private static final ForgeConfigSpec.IntValue MAX_INTERVAL;
    private static final ForgeConfigSpec.DoubleValue BOSS_HEALTH;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXEMPT_TAGS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXEMPT_MOBS;
    private static final ForgeConfigSpec.IntValue HOPPER_REST;
    private static final ForgeConfigSpec.IntValue AFK_SECONDS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SLOW_SCRIPTS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SLOW_FUNCTIONS;

    public static int maxInterval = 16;
    public static double bossHealth = 100;
    public static List<String> exemptTags = List.of();
    public static List<String> exemptMobs = List.of();
    public static int hopperRest = 8;
    public static int afkSeconds = 60;
    public static List<String> slowScripts = List.of();
    public static List<String> slowFunctions = List.of();

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Char's Boost. /charboost status shows what each part is doing.").push("general");
        ENABLED = b.comment("Emergency switch. false = the mod hooks nothing at all from the next restart on, and stops at once if the server is running.")
                .define("enabled", true);
        TARGET = b.comment("Milliseconds a tick may take before throttling starts. 50 is the most a tick can take at a full 20 TPS.")
                .defineInRange("targetMspt", 45.0, 5.0, 500.0);
        MARGIN = b.comment("Throttling eases off once ticks are this far under the target.")
                .defineInRange("marginMspt", 10.0, 1.0, 100.0);
        RAISE = b.comment("Seconds over the target before throttling goes up one step.")
                .defineInRange("raiseAfterSeconds", 5, 1, 600);
        LOWER = b.comment("Seconds under the target before throttling goes down one step.")
                .defineInRange("lowerAfterSeconds", 20, 1, 3600);
        b.pop();

        b.comment("Each optimisation on its own. false in the file = its hooks aren't even put in on the next start. /charboost off <name> stops one straight away.")
                .push("switches");
        for (Opt o : Opt.values()) {
            SWITCHES.put(o, b.comment(o.about + (o.loadOnly ? " (only while under load)" : " (always)")).define(o.key, true));
        }
        b.pop();

        b.comment("Mob AI throttling. Players are never touched by it.").push("mobs");
        MAX_INTERVAL = b.comment("The slowest a far mob's AI can go, in ticks.").defineInRange("maxInterval", 16, 1, 64);
        BOSS_HEALTH = b.comment("Mobs with at least this much max health count as bosses and are left alone.")
                .defineInRange("bossHealth", 100.0, 1.0, 100000.0);
        EXEMPT_TAGS = b.comment("Mobs with any of these tags are left alone.")
                .defineListAllowEmpty(List.of("exemptTags"), () -> List.of("charpowers.clone", "charpowers.held", "charboost.exempt"), o -> o instanceof String);
        EXEMPT_MOBS = b.comment("Mob ids left alone. A whole mod: \"modid:*\".")
                .defineListAllowEmpty(List.of("exemptMobs"), () -> List.of("charpowers:*", "minecraft:warden", "minecraft:ender_dragon", "minecraft:wither", "minecraft:elder_guardian"), o -> o instanceof String);
        b.pop();

        b.push("hoppers");
        HOPPER_REST = b.comment("Most ticks an idle hopper rests. 8 is a hopper's normal pause between items, so a working line of hoppers runs at full speed.")
                .defineInRange("maxRest", 8, 0, 8);
        b.pop();

        b.push("afk");
        AFK_SECONDS = b.comment("Seconds without moving, looking round, clicking or using a power before a player counts as AFK.")
                .defineInRange("afkSeconds", 60, 5, 3600);
        b.pop();

        b.comment("The slow lists. Names as /charboost scripts and /charboost functions print them. Part of a name is enough.").push("slow");
        SLOW_SCRIPTS = b.comment("KubeJS tick scripts that may run less often while under load. Only list scripts that keep working when they miss ticks.")
                .defineListAllowEmpty(List.of("scripts"), () -> DefaultSlowLists.SCRIPTS, o -> o instanceof String);
        SLOW_FUNCTIONS = b.comment("Datapack tick functions that may run less often while under load.")
                .defineListAllowEmpty(List.of("functions"), () -> DefaultSlowLists.FUNCTIONS, o -> o instanceof String);
        b.pop();
        SPEC = b.build();
    }

    private Cfg() {
    }

    public static void apply() {
        Boost.enabled = ENABLED.get();
        for (Opt o : Opt.values()) {
            o.on = SWITCHES.get(o).get();
        }
        Boost.tuner = new Tuner(TARGET.get(), MARGIN.get(), RAISE.get(), LOWER.get());
        maxInterval = MAX_INTERVAL.get();
        bossHealth = BOSS_HEALTH.get();
        exemptTags = copy(EXEMPT_TAGS.get());
        exemptMobs = copy(EXEMPT_MOBS.get());
        hopperRest = HOPPER_REST.get();
        afkSeconds = AFK_SECONDS.get();
        slowScripts = copy(SLOW_SCRIPTS.get());
        slowFunctions = copy(SLOW_FUNCTIONS.get());
    }

    private static List<String> copy(List<? extends String> in) {
        return new ArrayList<>(in);
    }

    /** Saves a switch from /charboost on|off so it survives a restart. */
    public static void save(Opt o) {
        ForgeConfigSpec.BooleanValue v = SWITCHES.get(o);
        v.set(o.on);
        v.save();
    }

    public static void saveEnabled() {
        ENABLED.set(Boost.enabled);
        ENABLED.save();
    }
}
