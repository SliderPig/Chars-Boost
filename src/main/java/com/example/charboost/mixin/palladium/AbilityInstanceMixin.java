package com.example.charboost.mixin.palladium;

import com.example.charboost.Opt;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.context.DataContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Each ability built a new little map of "who am I" up to twice a tick just to check its conditions, and a player with a
 * few big powers has hundreds of abilities. Each ability now keeps one and only remakes it if the entity or dimension changed.
 * Pressing a power key also counts as not being AFK.
 */
@Mixin(value = AbilityInstance.class, remap = false)
public abstract class AbilityInstanceMixin {
    @Unique
    private DataContext charboost$context;
    @Unique
    private Object charboost$contextLevel;

    @Inject(method = "evaluateConditions", at = @At("HEAD"), cancellable = true)
    private void charboost$evaluate(List<Condition> conditions, LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        Opt.ABILITIES.seen++;
        if (!Opt.ABILITIES.live()) {
            return;
        }
        if (conditions.isEmpty()) {
            cir.setReturnValue(true);
            return;
        }
        DataContext context = charboost$context;
        Object level = entity.level();
        if (context == null || charboost$contextLevel != level || context.getEntity() != entity) {
            context = DataContext.forAbility(entity, (AbilityInstance) (Object) this);
            charboost$context = context;
            charboost$contextLevel = level;
        } else {
            Opt.ABILITIES.skips++;
            Opt.ABILITIES.savedNs += 150;
        }
        cir.setReturnValue(ConditionSerializer.checkConditions(conditions, context));
    }

    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void charboost$awake(LivingEntity entity, boolean pressed, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player) {
            player.resetLastActionTime();
        }
    }
}
