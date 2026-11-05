package com.example.charboost.mixin;

import com.example.charboost.hook.AsyncSaves;
import java.io.File;
import java.io.IOException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.saveddata.SavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** World data files (scoreboard, raids, maps, mods' data): gathered here, compressed and written on another core. */
@Mixin(SavedData.class)
public abstract class SavedDataMixin {
    @Redirect(method = "save(Ljava/io/File;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/io/File;)V"))
    private void charboost$write(CompoundTag data, File file) throws IOException {
        if (AsyncSaves.live()) {
            AsyncSaves.nbtFile(data, file);
        } else {
            NbtIo.writeCompressed(data, file);
        }
    }
}
