package com.example.charboost.hook;

import com.example.charboost.Opt;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.threetag.palladium.network.SyncPropertyMessage;
import net.threetag.palladium.util.property.EntityPropertyHandler;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.SyncType;

/**
 * Palladium tells every player in the dimension about a property each time it's set, even when it's set to what it already
 * was (only yes/no properties check). Scripts do things like "set to 0, then count back up to 3" every tick, which is a
 * packet per step per player. This holds them until the end of the tick and sends only the final value, and nothing at all if
 * it ended up where it started. What's a list or anything else that can change in place still goes straight out.
 */
public final class PropertySync {
    private static final class Pending {
        Object first;
        Object last;
    }

    private static final Reference2ObjectOpenHashMap<EntityPropertyHandler, Reference2ObjectOpenHashMap<PalladiumProperty<?>, Pending>> PENDING = new Reference2ObjectOpenHashMap<>();

    private PropertySync() {
    }

    private static boolean plain(Object value) {
        return value == null || value instanceof Number || value instanceof Boolean || value instanceof String;
    }

    /** True if this change is now in the queue, so Palladium shouldn't send it itself. */
    public static boolean hold(EntityPropertyHandler handler, PalladiumProperty<?> property, Object oldValue, Object newValue) {
        Opt.PROPERTIES.seen++;
        if (!Opt.PROPERTIES.live() || handler.getEntity().level().isClientSide || !plain(oldValue) || !plain(newValue)) {
            return false;
        }
        if (property.getSyncType() == SyncType.NONE) {
            return true;
        }
        Pending pending = PENDING.computeIfAbsent(handler, h -> new Reference2ObjectOpenHashMap<>(4)).get(property);
        if (pending == null) {
            pending = new Pending();
            pending.first = oldValue;
            PENDING.get(handler).put(property, pending);
        } else {
            // One packet saved for each player in the dimension.
            Opt.PROPERTIES.skips++;
            Opt.PROPERTIES.savedNs += 3_000;
        }
        pending.last = newValue;
        return true;
    }

    /** End of the tick: send what really changed. */
    public static void flush() {
        if (PENDING.isEmpty()) {
            return;
        }
        for (Reference2ObjectMap.Entry<EntityPropertyHandler, Reference2ObjectOpenHashMap<PalladiumProperty<?>, Pending>> byHandler : PENDING.reference2ObjectEntrySet()) {
            Entity entity = byHandler.getKey().getEntity();
            for (Reference2ObjectMap.Entry<PalladiumProperty<?>, Pending> change : byHandler.getValue().reference2ObjectEntrySet()) {
                Pending pending = change.getValue();
                if (Objects.equals(pending.first, pending.last) || entity.isRemoved()) {
                    Opt.PROPERTIES.skips++;
                    Opt.PROPERTIES.savedNs += 3_000;
                    continue;
                }
                PalladiumProperty<?> property = change.getKey();
                if (property.getSyncType() == SyncType.EVERYONE) {
                    new SyncPropertyMessage(entity.getId(), property, pending.last).sendToDimension(entity.level());
                } else if (property.getSyncType() == SyncType.SELF && entity instanceof ServerPlayer player) {
                    new SyncPropertyMessage(entity.getId(), property, pending.last).send(player);
                }
            }
        }
        PENDING.clear();
    }
}
