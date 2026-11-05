package com.example.charboost.mixin.palladium;

import com.example.charboost.Opt;
import com.example.charboost.hook.ConditionMemo;
import com.example.charboost.hook.Scripts;
import java.util.Map;
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
 * Conditions written in script. Puts them on the /charboost scripts list with everything else, and doesn't run a
 * read-only one again if this entity already got the answer this tick. See ConditionMemo.
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
    private Scripts.Entry charboost$entry;
    @Unique
    private int charboost$memoId = -2;

    @Unique
    private void charboost$setUp() {
        String name = String.valueOf(ConditionSerializer.REGISTRY.getKey(serializer));
        charboost$entry = Scripts.entry("condition " + name, 0);
        charboost$memoId = ConditionMemo.idFor(name, extraProperties);
    }

    @Inject(method = "active", at = @At("HEAD"), cancellable = true)
    private void charboost$start(DataContext context, CallbackInfoReturnable<Boolean> cir) {
        if (charboost$memoId == -2) {
            charboost$setUp();
        }
        LivingEntity entity = context.getLivingEntity();
        if (charboost$memoId >= 0 && entity != null && !entity.level().isClientSide && Opt.ABILITIES.live()) {
            byte known = ConditionMemo.get(entity, charboost$memoId);
            if (known >= 0) {
                Opt.ABILITIES.skips++;
                Opt.ABILITIES.savedNs += (long) charboost$entry.cost.average();
                cir.setReturnValue(known == 1);
                return;
            }
        }
        charboost$started = System.nanoTime();
    }

    @Inject(method = "active", at = @At("RETURN"))
    private void charboost$end(DataContext context, CallbackInfoReturnable<Boolean> cir) {
        // A remembered answer returns straight from the start above and never gets here.
        charboost$entry.cost.add(System.nanoTime() - charboost$started);
        LivingEntity entity = context.getLivingEntity();
        if (charboost$memoId >= 0 && entity != null && !entity.level().isClientSide && Opt.ABILITIES.live()) {
            ConditionMemo.put(entity, charboost$memoId, cir.getReturnValueZ());
        }
    }
}
