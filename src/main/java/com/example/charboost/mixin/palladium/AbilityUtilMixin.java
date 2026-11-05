package com.example.charboost.mixin.palladium;

import com.example.charboost.Opt;
import com.example.charboost.hook.HolderIndex;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.entity.PalladiumLivingEntityExtension;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.AbilityUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "Has this entity got an ability of type X on?" streamed through every ability of every power each time. It runs for every
 * block a player touches (intangibility), and for a lot of other things. Each power now keeps its abilities by type.
 * Same answers in the same order, without going through hundreds of abilities and making new lists.
 */
@Mixin(value = AbilityUtil.class, remap = false)
public abstract class AbilityUtilMixin {
    @Unique
    private static AbilityInstance[] charboost$of(IPowerHolder holder, Ability ability) {
        return holder instanceof HolderIndex index ? index.charboost$ofType(ability) : null;
    }

    @Unique
    private static void charboost$collect(LivingEntity entity, Ability ability, boolean enabledOnly, List<AbilityInstance> out) {
        if (!(entity instanceof PalladiumLivingEntityExtension ext)) {
            return;
        }
        for (IPowerHolder holder : ext.palladium$getPowerHandler().getPowerHolders().values()) {
            AbilityInstance[] of = charboost$of(holder, ability);
            if (of != null) {
                for (AbilityInstance instance : of) {
                    if (!enabledOnly || instance.isEnabled()) {
                        out.add(instance);
                    }
                }
            } else {
                for (AbilityInstance instance : holder.getAbilities().values()) {
                    if (instance.getConfiguration().getAbility() == ability && (!enabledOnly || instance.isEnabled())) {
                        out.add(instance);
                    }
                }
            }
        }
    }

    @Inject(method = "getInstances(Lnet/minecraft/world/entity/LivingEntity;Lnet/threetag/palladium/power/ability/Ability;)Ljava/util/Collection;", at = @At("HEAD"), cancellable = true)
    private static void charboost$instances(LivingEntity entity, Ability ability, CallbackInfoReturnable<Collection<AbilityInstance>> cir) {
        if (Opt.ABILITIES.live()) {
            List<AbilityInstance> out = new ArrayList<>(0);
            charboost$collect(entity, ability, false, out);
            cir.setReturnValue(out);
        }
    }

    @Inject(method = "getEnabledInstances(Lnet/minecraft/world/entity/LivingEntity;Lnet/threetag/palladium/power/ability/Ability;)Ljava/util/Collection;", at = @At("HEAD"), cancellable = true)
    private static void charboost$enabled(LivingEntity entity, Ability ability, CallbackInfoReturnable<Collection<AbilityInstance>> cir) {
        if (Opt.ABILITIES.live()) {
            List<AbilityInstance> out = new ArrayList<>(0);
            charboost$collect(entity, ability, true, out);
            cir.setReturnValue(out);
        }
    }

    @Inject(method = "isTypeEnabled(Lnet/minecraft/world/entity/LivingEntity;Lnet/threetag/palladium/power/ability/Ability;)Z", at = @At("HEAD"), cancellable = true)
    private static void charboost$typeEnabled(LivingEntity entity, Ability ability, CallbackInfoReturnable<Boolean> cir) {
        if (Opt.ABILITIES.live()) {
            List<AbilityInstance> out = new ArrayList<>(0);
            charboost$collect(entity, ability, true, out);
            cir.setReturnValue(!out.isEmpty());
        }
    }
}
