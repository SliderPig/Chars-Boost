package com.example.charboost.hook;

import com.example.charboost.Boost;
import com.example.charboost.Opt;
import com.example.charboost.model.TrackedTags;
import java.util.Set;
import net.minecraft.world.entity.Entity;

/**
 * Keeps count of which tags exist among loaded entities, so @e[tag=x] can answer "nobody" without looking.
 * It refuses to answer unless it has proof it's seeing every entity. If anything is off, the normal search runs.
 */
public final class Census {
    /** How long looking at one entity takes in a selector. A rough figure from timing, only used for the score. */
    public static final int NANOS_PER_ENTITY = 15;

    private static long attachedEver = 0;
    private static boolean broken = false;
    public static int loaded = 0;

    private Census() {
    }

    public static void attach(Object access) {
        if (!(access instanceof Entity entity) || entity.level().isClientSide) {
            return;
        }
        Set<String> tags = entity.getTags();
        if (tags instanceof TrackedTags tracked) {
            if (!tracked.attached()) {
                tracked.attach(Boost.CENSUS);
                loaded++;
                attachedEver++;
            }
        } else {
            // The tag hook didn't go in for this entity, so the count can't be trusted.
            broken = true;
        }
    }

    public static void detach(Object access) {
        if (!(access instanceof Entity entity) || entity.level().isClientSide) {
            return;
        }
        if (entity.getTags() instanceof TrackedTags tracked && tracked.attached()) {
            tracked.detach();
            loaded--;
        }
    }

    /** True only if the count is trustworthy and at least one of these tags is on nobody. */
    public static boolean nobodyCouldMatch(String[] requiredTags) {
        if (requiredTags.length == 0 || broken || attachedEver == 0 || !Opt.SELECTOR_TAGS.live()) {
            return false;
        }
        return !Boost.CENSUS.anyHaveAll(requiredTags);
    }

    public static boolean working() {
        return !broken && attachedEver > 0;
    }
}
