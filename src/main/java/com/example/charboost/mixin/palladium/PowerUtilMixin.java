package com.example.charboost.mixin.palladium;

import com.example.charboost.Opt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.entity.PalladiumLivingEntityExtension;
import net.threetag.palladium.power.PowerUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "Does this entity have this power?" went through every power one by one and made several objects each time, for every
 * entity in every selector like @e[palladium.power=x], and for every script that asks. A direct lookup gives the same answer.
 */
@Mixin(value = PowerUtil.class, remap = false)
public abstract class PowerUtilMixin {
    @Inject(method = "hasPower", at = @At("HEAD"), cancellable = true)
    private static void charboost$hasPower(LivingEntity entity, ResourceLocation powerId, CallbackInfoReturnable<Boolean> cir) {
        if (Opt.POWER_PROVIDERS.live()) {
            cir.setReturnValue(entity instanceof PalladiumLivingEntityExtension ext
                    && ext.palladium$getPowerHandler().getPowerHolders().containsKey(powerId));
        }
    }
}
