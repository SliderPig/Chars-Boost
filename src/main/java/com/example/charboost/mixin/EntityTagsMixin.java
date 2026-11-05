package com.example.charboost.mixin;

import com.example.charboost.model.TrackedTags;
import com.google.common.collect.Sets;
import java.util.HashSet;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Gives every entity a tag set that reports changes, see TrackedTags. It behaves exactly like the normal one. */
@Mixin(Entity.class)
public abstract class EntityTagsMixin {
    @SuppressWarnings("unchecked")
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newHashSet()Ljava/util/HashSet;", remap = false))
    private <E> HashSet<E> charboost$tags() {
        return (HashSet<E>) new TrackedTags();
    }
}
