package com.example.charboost.mixin;

import com.example.charboost.Boost;
import com.example.charboost.Opt;
import com.example.charboost.model.AiRules;
import com.example.charboost.model.Throttle;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every mob asks "should I despawn?" every tick, which fires a Forge event each time. Under load it only asks
 * some ticks, with the 1 in 800 chance raised to match, so mobs still clear out at the same rate.
 */
@Mixin(Mob.class)
public abstract class MobDespawnMixin {
    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
    private void charboost$despawn(CallbackInfo ci) {
        Opt.DESPAWN.seen++;
        if (Opt.DESPAWN.live() && !Throttle.keep(Boost.tick, ((Mob) (Object) this).getId(), AiRules.despawnEvery(Boost.level))) {
            Opt.DESPAWN.skips++;
            Opt.DESPAWN.savedNs += 300;
            ci.cancel();
        }
    }

    @ModifyConstant(method = "checkDespawn", constant = @Constant(intValue = 800))
    private int charboost$chance(int chance) {
        return Opt.DESPAWN.live() ? chance / AiRules.despawnEvery(Boost.level) : chance;
    }
}
