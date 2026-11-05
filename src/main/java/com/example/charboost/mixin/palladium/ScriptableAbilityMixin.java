package com.example.charboost.mixin.palladium;

import com.example.charboost.hook.Scripts;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.compat.kubejs.ability.ScriptableAbility;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Abilities written in script. Puts them on the /charboost scripts list with everything else. */
@Mixin(value = ScriptableAbility.class, remap = false)
public abstract class ScriptableAbilityMixin {
    @Unique
    private static long charboost$started;
    @Unique
    private Scripts.Entry charboost$entry;

    @Inject(method = "tick", at = @At("HEAD"))
    private void charboost$start(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled, CallbackInfo ci) {
        charboost$started = System.nanoTime();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void charboost$end(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled, CallbackInfo ci) {
        long took = System.nanoTime() - charboost$started;
        Scripts.Entry stat = charboost$entry;
        if (stat == null) {
            stat = Scripts.entry("ability " + Ability.REGISTRY.getKey((Ability) (Object) this), 0);
            charboost$entry = stat;
        }
        stat.cost.add(took);
    }
}
