package com.example.charboost.mixin;

import com.example.charboost.hook.AsyncSaves;
import java.io.File;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraftforge.event.ForgeEventFactory;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Player files: the data is gathered on the main thread as always, compressing and writing it happens on another core. */
@Mixin(PlayerDataStorage.class)
public abstract class PlayerDataStorageMixin {
    @Shadow
    @Final
    private File playerDir;
    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void charboost$save(Player player, CallbackInfo ci) {
        if (!AsyncSaves.live()) {
            return;
        }
        try {
            CompoundTag data = player.saveWithoutId(new CompoundTag());
            String uuid = player.getStringUUID();
            AsyncSaves.playerFile(playerDir, uuid, data);
            ForgeEventFactory.firePlayerSavingEvent(player, playerDir, uuid);
        } catch (Exception e) {
            LOGGER.warn("Failed to save player data for {}", player.getName().getString());
        }
        ci.cancel();
    }

    /** A player rejoining straight after leaving must read what was just saved. */
    @Inject(method = "load", at = @At("HEAD"))
    private void charboost$load(Player player, CallbackInfoReturnable<CompoundTag> cir) {
        AsyncSaves.waitForPlayer(player.getStringUUID());
    }
}
