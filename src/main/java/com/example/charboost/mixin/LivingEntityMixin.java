package com.example.charboost.mixin;

import com.example.charboost.hook.MobAi;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every living thing looks for things to push every tick. Far mobs don't need to. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void charboost$push(CallbackInfo ci) {
        if (MobAi.skipPush((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
