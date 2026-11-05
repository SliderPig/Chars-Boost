package com.example.charboost.mixin;

import com.example.charboost.hook.MobAi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Villagers, piglins, axolotls and so on think with a brain instead of goals. Far ones think less often, but
 * their memories still count down every tick, so timers like a villager's "golem seen recently" keep real time
 * and farms that lean on them keep working.
 */
@Mixin(Brain.class)
public abstract class BrainMixin<E extends LivingEntity> {
    @Shadow
    private void forgetOutdatedMemories() {
    }

    @Unique
    private long charboost$started;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void charboost$tickHead(ServerLevel level, E entity, CallbackInfo ci) {
        if (!MobAi.brainDue(entity)) {
            forgetOutdatedMemories();
            ci.cancel();
            return;
        }
        charboost$started = System.nanoTime();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void charboost$tickEnd(ServerLevel level, E entity, CallbackInfo ci) {
        MobAi.brainCost(System.nanoTime() - charboost$started);
    }
}
