package com.example.charboost.hook;

import com.example.charboost.Boost;
import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.world.entity.LivingEntity;

/**
 * Palladium checks an entity's abilities one after another, and the same scripted condition with the same settings comes
 * up over and over, like "isn't dampened" on nearly every ability of a power. Each one is a trip through the script engine.
 * Only conditions read through by hand and found to just look something up are remembered, and only for this one
 * entity's turn in this one tick.
 */
public final class ConditionMemo {
    /** Conditions that only read the world and change nothing. Each was read through by hand. */
    private static final Set<String> PURE = Set.of(
            "hertz:dampened_by", "hertz:safe_has_effect_check", "fsang:dampened_by",
            "fsang:is_mainhand", "heroes:is_mainhand", "gabeupheroes:mainhand",
            "fsang:get_energies", "fsang:get_energy_condition", "fsang:get_energy", "fsang:get_energy_range",
            "fsang:has_power_in_namespace", "gabeupheroes:has_power_in_namespace", "heroes:has_powers",
            "fsang:power_selected", "eop:power_selected", "powerborne:not_dampened");

    private static final Map<String, Integer> IDS = new HashMap<>();
    private static final Int2ByteOpenHashMap RESULTS = new Int2ByteOpenHashMap();
    private static LivingEntity entity;
    private static long tick = -1;

    static {
        RESULTS.defaultReturnValue((byte) -1);
    }

    private ConditionMemo() {
    }

    /** A number for this condition plus settings, or -1 if it must always really run. */
    public static int idFor(String condition, Map<String, Object> settings) {
        if (!PURE.contains(condition)) {
            return -1;
        }
        Map<String, Object> sorted = new TreeMap<>(settings);
        StringBuilder key = new StringBuilder(condition);
        for (Map.Entry<String, Object> e : sorted.entrySet()) {
            key.append('|').append(e.getKey()).append('=');
            Object value = e.getValue();
            key.append(value instanceof Object[] array ? Arrays.deepToString(array) : String.valueOf(value));
        }
        return IDS.computeIfAbsent(key.toString(), k -> IDS.size());
    }

    private static void turn(LivingEntity who) {
        if (who != entity || Boost.tick != tick) {
            entity = who;
            tick = Boost.tick;
            RESULTS.clear();
        }
    }

    /** 1 = true, 0 = false, -1 = not known yet. */
    public static byte get(LivingEntity who, int id) {
        turn(who);
        return RESULTS.get(id);
    }

    public static void put(LivingEntity who, int id, boolean result) {
        turn(who);
        RESULTS.put(id, (byte) (result ? 1 : 0));
    }
}
