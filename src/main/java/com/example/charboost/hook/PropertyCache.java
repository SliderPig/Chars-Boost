package com.example.charboost.hook;

import com.example.charboost.Opt;
import com.example.charboost.model.RollingCost;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventResult;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.threetag.palladium.compat.kubejs.RegisterPalladiumPropertyEventJS;
import net.threetag.palladium.util.property.EntityPropertyHandler;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * Every new entity, even a dropped item or an arrow, runs every script's PalladiumEvents.registerProperties to get its list
 * of properties: hundreds of script calls each time. Those scripts can only look at the entity's type, so the answer is the
 * same for every entity of a type. The first one of each type runs the scripts and the answer is written down; the
 * rest copy it. Script reloads throw the answers away.
 */
public final class PropertyCache {
    /** Each entry is property, value, property, value... */
    private static final Reference2ObjectOpenHashMap<EntityType<?>, Object[]> ANSWERS = new Reference2ObjectOpenHashMap<>();
    private static final RollingCost COST = new RollingCost();
    private static Object headServer;
    private static Object headStartup;
    private static int generation = -1;
    private static List<Object> recording;
    private static boolean canKeep;

    private PropertyCache() {
    }

    public static EventResult post(EventHandler handler, RegisterPalladiumPropertyEventJS event) {
        RegisterEventAccess access = (RegisterEventAccess) (Object) event;
        Entity entity = access.charboost$entity();
        if (!Opt.PROPERTIES.live() || entity.level().isClientSide || recording != null) {
            return handler.post(event);
        }
        Object[] heads = ((EventHandlerHeads) (Object) handler).charboost$heads();
        if (heads[0] != headServer || heads[1] != headStartup || generation != PowersHook.generation) {
            ANSWERS.clear();
            headServer = heads[0];
            headStartup = heads[1];
            generation = PowersHook.generation;
        }
        EntityType<?> type = entity.getType();
        Object[] answer = ANSWERS.get(type);
        if (answer != null) {
            EntityPropertyHandler target = access.charboost$handler();
            for (int i = 0; i < answer.length; i += 2) {
                register(target, answer[i], answer[i + 1]);
            }
            Opt.PROPERTIES.skips++;
            Opt.PROPERTIES.savedNs += (long) COST.average();
            return EventResult.PASS;
        }
        recording = new ArrayList<>();
        canKeep = true;
        long start = System.nanoTime();
        try {
            EventResult result = handler.post(event);
            COST.add(System.nanoTime() - start);
            if (canKeep) {
                ANSWERS.put(type, recording.toArray());
            }
            return result;
        } finally {
            recording = null;
        }
    }

    /** How many entity types have their property list written down. */
    public static int types() {
        return ANSWERS.size();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void register(EntityPropertyHandler target, Object property, Object value) {
        target.register((PalladiumProperty) property, value);
    }

    /** Called for each property a script registers, while an answer is being written down. */
    public static void note(PalladiumProperty<?> property, Object value) {
        if (recording == null) {
            return;
        }
        recording.add(property);
        recording.add(value);
        // A value that can change would end up shared between entities. All the mods' ones can't, but check.
        if (!(value == null || value instanceof Boolean || value instanceof Number || value instanceof String)) {
            canKeep = false;
        }
    }
}
