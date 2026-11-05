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
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Each ability built a new little map of "who am I" up to twice a tick just to check its conditions. A player with a
 * few big powers has hundreds of abilities. Each ability now keeps one and only updates what can change.
 * Pressing a power key also counts as not being AFK.
 */
@Mixin(value = AbilityInstance.class, remap = false)
public abstract class AbilityInstanceMixin {
    @Unique
    private DataContext charboost$context;
    @Unique
    private Object charboost$contextLevel;

    /**
     * @author Char
     * @reason Same check, without building a new context every time.
     */
    @Overwrite
    private boolean evaluateConditions(List<Condition> conditions, LivingEntity entity) {
        if (conditions.isEmpty()) {
            return true;
        }
        Opt.ABILITY_CONTEXT.seen++;
        DataContext context = charboost$context;
        Object level = entity.level();
        if (context == null || !Opt.ABILITY_CONTEXT.live() || charboost$contextLevel != level || context.getEntity() != entity) {
            context = DataContext.forAbility(entity, (AbilityInstance) (Object) this);
            charboost$context = context;
            charboost$contextLevel = level;
        } else {
            Opt.ABILITY_CONTEXT.skips++;
            Opt.ABILITY_CONTEXT.savedNs += 150;
        }
        return ConditionSerializer.checkConditions(conditions, context);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void charboost$awake(LivingEntity entity, boolean pressed, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player) {
            player.resetLastActionTime();
        }
    }
}
