package com.example.charboost.mixin;

import com.example.charboost.Boost;
import com.example.charboost.Opt;
import com.example.charboost.hook.MobAi;
import com.example.charboost.model.AiRules;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Path searches are the single most expensive thing a mob does. Far mobs search less hard, and a mob that just
 * found no way to a spot waits a moment before searching for the same spot again.
 */
@Mixin(PathNavigation.class)
public abstract class PathNavigationMixin {
    @Shadow
    @Final
    protected Mob mob;

    @Unique
    private int charboost$failedTargets;
    @Unique
    private long charboost$failedAt = Long.MIN_VALUE / 2;

    @Inject(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true)
    private void charboost$wait(Set<BlockPos> targets, int range, boolean above, int accuracy, float followRange, CallbackInfoReturnable<Path> cir) {
        Opt.PATHS.seen++;
        if (!Opt.PATHS.live() || targets.isEmpty()) {
            return;
        }
        // Only when the mob isn't already walking a good path there, which the game answers by itself.
        if (Boost.tick - charboost$failedAt < AiRules.pathRetryTicks(Boost.level) && targets.hashCode() == charboost$failedTargets) {
            Opt.PATHS.skips++;
            Opt.PATHS.savedNs += 60_000;
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("RETURN"))
    private void charboost$remember(Set<BlockPos> targets, int range, boolean above, int accuracy, float followRange, CallbackInfoReturnable<Path> cir) {
        Path path = cir.getReturnValue();
        if ((path == null || !path.canReach()) && !targets.isEmpty()) {
            // A wait that just ended leaves the old time in place until a real search fails again.
            if (Boost.tick - charboost$failedAt >= AiRules.pathRetryTicks(Boost.level)) {
                charboost$failedAt = Boost.tick;
                charboost$failedTargets = targets.hashCode();
            }
        } else {
            charboost$failedAt = Long.MIN_VALUE / 2;
        }
    }

    @ModifyArg(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/pathfinder/PathFinder;findPath(Lnet/minecraft/world/level/PathNavigationRegion;Lnet/minecraft/world/entity/Mob;Ljava/util/Set;FIF)Lnet/minecraft/world/level/pathfinder/Path;"),
            index = 5)
    private float charboost$depth(float multiplier) {
        if (!Opt.PATHS.live()) {
            return multiplier;
        }
        return multiplier * AiRules.pathDepth(MobAi.interval(mob));
    }
}
