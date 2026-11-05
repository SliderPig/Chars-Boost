package com.example.charboost.mixin.palladium;

import com.example.charboost.hook.Afk;
import com.example.charboost.hook.HolderIndex;
import com.example.charboost.hook.MobAi;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.threetag.palladium.power.DefaultPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An AFK player's powers, and far mobs' powers, tick less often while the server is struggling. Also keeps a power's abilities by type. */
@Mixin(value = DefaultPowerHolder.class, remap = false)
public abstract class DefaultPowerHolderMixin implements HolderIndex {
    @Unique
    private static final AbilityInstance[] charboost$NONE = new AbilityInstance[0];

    @Shadow
    @Final
    public LivingEntity entity;
    @Shadow
    @Final
    private ImmutableMap<String, AbilityInstance> abilityInstances;

    @Unique
    private Reference2ObjectOpenHashMap<Ability, AbilityInstance[]> charboost$byType;

    @Override
    public AbilityInstance[] charboost$ofType(Ability ability) {
        Reference2ObjectOpenHashMap<Ability, AbilityInstance[]> index = charboost$byType;
        if (index == null) {
            // A holder's abilities never change, so this only has to be worked out once.
            Reference2ObjectOpenHashMap<Ability, List<AbilityInstance>> lists = new Reference2ObjectOpenHashMap<>();
            for (AbilityInstance instance : abilityInstances.values()) {
                lists.computeIfAbsent(instance.getConfiguration().getAbility(), a -> new ArrayList<>(2)).add(instance);
            }
            index = new Reference2ObjectOpenHashMap<>(lists.size());
            for (var entry : lists.reference2ObjectEntrySet()) {
                index.put(entry.getKey(), entry.getValue().toArray(new AbilityInstance[0]));
            }
            charboost$byType = index;
        }
        AbilityInstance[] found = index.get(ability);
        return found == null ? charboost$NONE : found;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void charboost$afk(CallbackInfo ci) {
        LivingEntity self = entity;
        if (self instanceof Mob mob ? !mob.level().isClientSide && MobAi.skipPowers(mob) : Afk.skipPowers(self, 7)) {
            ci.cancel();
        }
    }
}
