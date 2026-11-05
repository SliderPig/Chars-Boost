package com.example.charboost.mixin;

import com.example.charboost.hook.MobAi;
import com.example.charboost.hook.MobState;
import java.util.List;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Mob AI: far mobs run their goals less often. Their walking still runs every tick. */
@Mixin(Mob.class)
public abstract class MobMixin implements MobState {
    @Shadow
    @Final
    public GoalSelector targetSelector;

    @Unique
    private int charboost$interval = 1;
    @Unique
    private long charboost$nextLook = 0;
    @Unique
    private long[] charboost$last;

    @Override
    public int charboost$interval() {
        return charboost$interval;
    }

    @Override
    public void charboost$interval(int interval) {
        charboost$interval = interval;
    }

    @Override
    public long charboost$nextLook() {
        return charboost$nextLook;
    }

    @Override
    public void charboost$nextLook(long tick) {
        charboost$nextLook = tick;
    }

    @Override
    public long[] charboost$last() {
        if (charboost$last == null) {
            charboost$last = new long[MobAi.SLOTS];
        }
        return charboost$last;
    }

    @Redirect(method = "serverAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;tick()V"))
    private void charboost$goals(GoalSelector selector) {
        MobAi.goals((Mob) (Object) this, selector, selector == targetSelector ? MobAi.TARGET_GOALS : MobAi.GOALS);
    }

    @Redirect(method = "serverAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;tickRunningGoals(Z)V"))
    private void charboost$running(GoalSelector selector, boolean all) {
        MobAi.running((Mob) (Object) this, selector, all, selector == targetSelector ? MobAi.TARGET_RUNNING : MobAi.RUNNING);
    }

    /** Zombies, skeletons and so on look for items to pick up every tick. */
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private <T extends net.minecraft.world.entity.Entity> List<T> charboost$pickup(Level level, Class<T> type, AABB box) {
        return MobAi.pickupDue((Mob) (Object) this) ? level.getEntitiesOfClass(type, box) : List.of();
    }
}
