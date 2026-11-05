package com.example.charboost.mixin.palladium;

import com.example.charboost.hook.Afk;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.DefaultPowerHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An AFK player's powers tick less often while the server is struggling. */
@Mixin(value = DefaultPowerHolder.class, remap = false)
public abstract class DefaultPowerHolderMixin {
    @Shadow
    @Final
    public LivingEntity entity;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void charboost$afk(CallbackInfo ci) {
        if (Afk.skipPowers(entity, 7)) {
            ci.cancel();
        }
    }
}
