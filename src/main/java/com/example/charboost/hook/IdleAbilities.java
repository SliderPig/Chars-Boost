package com.example.charboost.hook;

import com.example.charboost.CharBoost;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Scripted abilities whose tick does nothing while the ability is off. Palladium calls them every tick anyway, on or off, and
 * each call is a trip through the script engine. The list is made by tools/make_idle_abilities.py reading the scripts.
 */
public final class IdleAbilities {
    private static Map<String, String[]> list;

    private IdleAbilities() {
    }

    private static Map<String, String[]> list() {
        if (list == null) {
            list = new HashMap<>();
            try (InputStream in = IdleAbilities.class.getResourceAsStream("/charboost_idle_abilities.txt")) {
                if (in != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.trim().split(" ");
                        if (parts.length == 4) {
                            list.put(parts[0], parts);
                        }
                    }
                }
            } catch (Exception e) {
                CharBoost.LOG.warn("[CharBoost] Couldn't read the idle ability list: {}", e.toString());
            }
        }
        return list;
    }

    /** True if it's on the list and its script is still the one that was read. */
    public static boolean idleWhenOff(String ability) {
        String[] parts = list().get(ability);
        return parts != null && ScriptGuard.same(parts[1], parts[2], parts[3]);
    }
}
