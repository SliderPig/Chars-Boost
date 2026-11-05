package com.example.charboost;

import java.util.List;

/** What ships on the slow lists. Filled in from real timings on the Powers mods, see the README. */
final class DefaultSlowLists {
    /**
     * Picked from real timings by reading each script: all bookkeeping that catches up by itself, nothing a player
     * feels from one tick to the next. Worst case, skill XP from playtime comes in a bit slower while the server is overloaded.
     */
    static final List<String> SCRIPTS = List.of(
            "fsang_hero:kubejs_scripts/exp_skill_system.js:478",
            "fsang_hero:kubejs_scripts/potion_handling.js:99",
            "fsang_hero:kubejs_scripts/mutation_assigner.js:321",
            "fsang_hero:kubejs_scripts/atlantean_assigner.js:44",
            "fsang_hero:kubejs_scripts/speedforce_assigner.js:65",
            "fsang_hero:kubejs_scripts/ichor_weapons.js:24",
            "eop:kubejs_scripts/power_amount.js:8",
            "eop:kubejs_scripts/power_registry.js:34");
    static final List<String> FUNCTIONS = List.of();

    private DefaultSlowLists() {
    }
}
