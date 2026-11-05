package com.example.charboost.hook;

import com.example.charboost.Opt;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * @e[type=item,nbt={Item:{id:"x"}}]: for each one of these the game saves every dropped item in full just to read its id, and
 * FSang runs four a tick. This asks the items directly first. If none is that kind, there's nothing to find.
 */
public final class SelectorItems {
    private static final Map<String, Item> ITEMS = new HashMap<>();
    /** How long saving one dropped item to NBT takes, roughly. Only used for the score. */
    private static final int NANOS_PER_ITEM = 20_000;

    private SelectorItems() {
    }

    /** The item with exactly this id, or null if nothing can ever match it: the check compares the id text exactly. */
    private static Item item(String id) {
        return ITEMS.computeIfAbsent(id, k -> {
            ResourceLocation key = ResourceLocation.tryParse(k);
            return key != null && key.toString().equals(k) && BuiltInRegistries.ITEM.containsKey(key) ? BuiltInRegistries.ITEM.get(key) : null;
        });
    }

    public static boolean noneInLevel(ServerLevel level, String id) {
        Item target = item(id);
        int[] looked = new int[1];
        boolean[] found = new boolean[1];
        if (target != null) {
            level.getEntities().get(EntityType.ITEM, entity -> {
                looked[0]++;
                if (entity.getItem().getItem() == target) {
                    found[0] = true;
                    return AbortableIterationConsumer.Continuation.ABORT;
                }
                return AbortableIterationConsumer.Continuation.CONTINUE;
            });
        }
        if (found[0]) {
            return false;
        }
        Opt.SELECTOR_TAGS.savedNs += (long) looked[0] * NANOS_PER_ITEM;
        return true;
    }
}
