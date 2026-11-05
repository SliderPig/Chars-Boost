package com.example.charboost.mixin.palladium;

import com.example.charboost.Opt;
import com.example.charboost.hook.IdleAbilities;
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

/**
 * Abilities written in script. Palladium calls their tick every tick, on or off. Most do nothing while off, so that call is
 * skipped for those (see IdleAbilities). Also puts them on the /charboost scripts list with everything else.
 */
@Mixin(value = ScriptableAbility.class, remap = false)
public abstract class ScriptableAbilityMixin {
    @Unique
    private static long charboost$started;
    @Unique
    private Scripts.Entry charboost$entry;
    @Unique
    private boolean charboost$idle;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void charboost$start(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled, CallbackInfo ci) {
        if (charboost$entry == null) {
            String id = String.valueOf(Ability.REGISTRY.getKey((Ability) (Object) this));
            charboost$entry = Scripts.entry("ability " + id, 0);
            charboost$idle = IdleAbilities.idleWhenOff(id);
        }
        Opt.ABILITIES.seen++;
        if (!enabled && charboost$idle && Opt.ABILITIES.live()) {
            Opt.ABILITIES.skips++;
            Opt.ABILITIES.savedNs += 8_000;
            ci.cancel();
            return;
        }
        charboost$started = System.nanoTime();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void charboost$end(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled, CallbackInfo ci) {
        charboost$entry.cost.add(System.nanoTime() - charboost$started);
    }
}
