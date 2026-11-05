package com.example.charboost.mixin;

import com.example.charboost.Boost;
import com.example.charboost.Cfg;
import com.example.charboost.Opt;
import com.example.charboost.model.Rates;
import com.example.charboost.model.RollingCost;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * An idle hopper hunts for items and containers every single tick. Under load, one that found nothing rests a few
 * ticks, using the game's own hopper cooldown. A hopper that moved something keeps its normal timing, and one
 * that receives an item resets the rest, so a working line of hoppers runs at full speed.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperMixin {
    @Unique
    private static final RollingCost charboost$cost = new RollingCost();
    @Unique
    private static long charboost$started;

    @Shadow
    private int cooldownTime;

    @Inject(method = "pushItemsTick", at = @At("HEAD"))
    private static void charboost$head(Level level, BlockPos pos, BlockState state, HopperBlockEntity hopper, CallbackInfo ci) {
        if (Opt.HOPPERS.live()) {
            charboost$started = System.nanoTime();
        }
    }

    @Inject(method = "pushItemsTick", at = @At("RETURN"))
    private static void charboost$rest(Level level, BlockPos pos, BlockState state, HopperBlockEntity hopper, CallbackInfo ci) {
        Opt.HOPPERS.seen++;
        if (!Opt.HOPPERS.live() || level.isClientSide) {
            return;
        }
        if (((HopperMixin) (Object) hopper).cooldownTime > 0) {
            // It moved something, or it's still in its pause.
            return;
        }
        charboost$cost.add(System.nanoTime() - charboost$started);
        boolean near = level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 32);
        int rest = Rates.hopperRest(Boost.level, near, Cfg.hopperRest);
        if (rest > 0) {
            hopper.setCooldown(rest);
            Opt.HOPPERS.skips++;
            Opt.HOPPERS.savedNs += (long) (charboost$cost.average() * (rest - 1));
        }
    }
}
