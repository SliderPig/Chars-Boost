package com.example.charboost.mixin.kubejs;

import com.example.charboost.hook.Afk;
import dev.latvian.mods.kubejs.player.KubeJSPlayerEventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** PlayerEvents.tick: an AFK player's whole set of tick scripts runs less often while the server is struggling. */
@Mixin(value = KubeJSPlayerEventHandler.class, remap = false)
public abstract class KubeJSPlayerEventHandlerMixin {
    @Unique
    private static long charboost$started;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private static void charboost$tick(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer server) {
            if (Afk.skipScripts(server)) {
                ci.cancel();
                return;
            }
            charboost$started = System.nanoTime();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private static void charboost$end(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer) {
            Afk.scriptCost(System.nanoTime() - charboost$started);
        }
    }
}
