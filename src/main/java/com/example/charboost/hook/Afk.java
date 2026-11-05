package com.example.charboost.hook;

import com.example.charboost.Boost;
import com.example.charboost.Cfg;
import com.example.charboost.Opt;
import com.example.charboost.model.Rates;
import com.example.charboost.model.RollingCost;
import com.example.charboost.model.Throttle;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Tells which players are AFK. Their tick scripts and powers run less often while the server is struggling.
 * Moving, looking round, clicking or pressing a power key wakes them straight away.
 */
public final class Afk {
    private static final RollingCost SCRIPT_COST = new RollingCost();
    private static final String CLONE_TAG = "charpowers.clone";

    private Afk() {
    }

    public static boolean isAfk(ServerPlayer player) {
        if (player.hurtTime > 0 || player.isDeadOrDying()) {
            return false;
        }
        if (Util.getMillis() - player.getLastActionTime() < Cfg.afkSeconds * 1000L) {
            return false;
        }
        // Clones never send anything, but they're busy.
        Set<String> tags = player.getTags();
        return tags.isEmpty() || !tags.contains(CLONE_TAG);
    }

    /** Should this AFK player's whole tick scripts/powers be skipped this tick? */
    public static boolean skipScripts(ServerPlayer player) {
        Opt.AFK.seen++;
        if (!Opt.AFK.live() || !isAfk(player)) {
            return false;
        }
        if (Throttle.keep(Boost.tick, player.getId(), Rates.afkOneIn(Boost.level))) {
            return false;
        }
        Opt.AFK.skips++;
        Opt.AFK.savedNs += (long) SCRIPT_COST.average();
        return true;
    }

    public static void scriptCost(long nanos) {
        SCRIPT_COST.add(nanos);
    }

    /** Should this holder of powers skip its ability ticks this tick? */
    public static boolean skipPowers(LivingEntity entity, int salt) {
        if (!(entity instanceof ServerPlayer player) || !Opt.AFK.live() || !isAfk(player)) {
            return false;
        }
        return !Throttle.keep(Boost.tick, player.getId() + salt, Rates.afkOneIn(Boost.level));
    }
}
