package com.example.charboost.mixin.palladium;

import com.example.charboost.Opt;
import com.example.charboost.hook.ConditionMemo;
import com.example.charboost.hook.NativeConditions;
import com.example.charboost.hook.Scripts;
import java.util.Map;
import java.util.function.BiPredicate;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.compat.kubejs.condition.ScriptableCondition;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.util.context.DataContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Conditions written in script. Puts them on the /charboost scripts list with everything else, doesn't run a read-only one
 * again if this entity already got the answer this tick (see ConditionMemo), and answers a few in Java (see NativeConditions).
 */
@Mixin(value = ScriptableCondition.class, remap = false)
public abstract class ScriptableConditionMixin {
    @Unique
    private static long charboost$started;

    @Shadow
    @Final
    public ConditionSerializer serializer;
    @Shadow
    @Final
    public Map<String, Object> extraProperties;

    @Unique
    private String charboost$name;
    @Unique
    private Scripts.Entry charboost$entry;
    @Unique
    private int charboost$memoId;
    @Unique
    private BiPredicate<LivingEntity, Map<String, Object>> charboost$java;

    @Inject(method = "active", at = @At("HEAD"), cancellable = true)
    private void charboost$start(DataContext context, CallbackInfoReturnable<Boolean> cir) {
        if (charboost$name == null) {
            charboost$name = String.valueOf(ConditionSerializer.REGISTRY.getKey(serializer));
            charboost$entry = Scripts.entry("condition " + charboost$name, 0);
            charboost$memoId = ConditionMemo.idFor(charboost$name, extraProperties);
            charboost$java = NativeConditions.find(charboost$name);
        }
        LivingEntity entity = context.getLivingEntity();
        if (entity != null && !entity.level().isClientSide && Opt.ABILITIES.live()) {
            if (charboost$memoId >= 0) {
                byte known = ConditionMemo.get(entity, charboost$memoId);
                if (known >= 0) {
                    Opt.ABILITIES.skips++;
                    Opt.ABILITIES.savedNs += (long) charboost$entry.cost.average();
                    cir.setReturnValue(known == 1);
                    return;
                }
            }
            if (charboost$java != null && NativeConditions.handles(charboost$name, entity, extraProperties)) {
                long start = System.nanoTime();
                boolean result = charboost$java.test(entity, extraProperties);
                charboost$entry.cost.add(System.nanoTime() - start);
                if (charboost$memoId >= 0) {
                    ConditionMemo.put(entity, charboost$memoId, result);
                }
                cir.setReturnValue(result);
                return;
            }
        }
        charboost$started = System.nanoTime();
    }

    @Inject(method = "active", at = @At("RETURN"))
    private void charboost$end(DataContext context, CallbackInfoReturnable<Boolean> cir) {
        // Answers from the memo or Java return straight from the start above and never get here.
        charboost$entry.cost.add(System.nanoTime() - charboost$started);
        LivingEntity entity = context.getLivingEntity();
        if (charboost$memoId >= 0 && entity != null && !entity.level().isClientSide && Opt.ABILITIES.live()) {
            ConditionMemo.put(entity, charboost$memoId, cir.getReturnValueZ());
        }
    }
}
