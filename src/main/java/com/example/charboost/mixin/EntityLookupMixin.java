package com.example.charboost.mixin;

import com.example.charboost.Opt;
import com.example.charboost.hook.Census;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import java.util.Map;
import java.util.UUID;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The list of all loaded entities in a dimension. Keeps a second list per entity type next to it, always in step, so
 * "every entity of type X" only looks at those. FSang alone has over 100 lines a tick like
 * execute as @e[type=palladium:custom_projectile,tag=...], and each used to look at every entity in every dimension.
 */
@Mixin(EntityLookup.class)
public abstract class EntityLookupMixin<T extends EntityAccess> {
    @Shadow
    @Final
    private Int2ObjectMap<T> byId;
    @Shadow
    @Final
    private Map<UUID, T> byUuid;

    @Unique
    private final Reference2ObjectOpenHashMap<EntityType<?>, ReferenceLinkedOpenHashSet<T>> charboost$byType = new Reference2ObjectOpenHashMap<>();

    @Inject(method = "add", at = @At("RETURN"))
    private void charboost$added(T entity, CallbackInfo ci) {
        // The game refuses a second entity with the same UUID, so check it really went in.
        if (byUuid.get(entity.getUUID()) == entity) {
            if (entity instanceof Entity real) {
                charboost$byType.computeIfAbsent(real.getType(), t -> new ReferenceLinkedOpenHashSet<>()).add(entity);
            }
            Census.attach(entity);
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void charboost$removed(T entity, CallbackInfo ci) {
        // The game removes by UUID and by id, whichever entity those lead to. Do exactly the same.
        charboost$forget(entity);
        T sameUuid = byUuid.get(entity.getUUID());
        if (sameUuid != null && sameUuid != entity) {
            charboost$forget(sameUuid);
        }
        T sameId = byId.get(entity.getId());
        if (sameId != null && sameId != entity && sameId != sameUuid) {
            charboost$forget(sameId);
        }
    }

    @Unique
    private void charboost$forget(T entity) {
        if (entity instanceof Entity real) {
            ReferenceLinkedOpenHashSet<T> set = charboost$byType.get(real.getType());
            if (set != null) {
                set.remove(entity);
            }
        }
        Census.detach(entity);
    }

    @Inject(method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/util/AbortableIterationConsumer;)V", at = @At("HEAD"), cancellable = true)
    private <U extends T> void charboost$fast(EntityTypeTest<T, U> test, AbortableIterationConsumer<U> consumer, CallbackInfo ci) {
        if (!(test instanceof EntityType<?> type)) {
            return;
        }
        Opt.SELECTOR_INDEX.seen++;
        if (!Opt.SELECTOR_INDEX.live()) {
            return;
        }
        ReferenceLinkedOpenHashSet<T> set = charboost$byType.get(type);
        int looked = 0;
        if (set != null) {
            looked = set.size();
            for (T entity : set) {
                U cast = test.tryCast(entity);
                if (cast != null && consumer.accept(cast).shouldAbort()) {
                    break;
                }
            }
        }
        Opt.SELECTOR_INDEX.skips++;
        Opt.SELECTOR_INDEX.savedNs += (long) (byId.size() - looked) * Census.NANOS_PER_ENTITY;
        ci.cancel();
    }
}
