package com.example.charboost.mixin;

import com.example.charboost.hook.AsyncSaves;
import java.io.File;
import java.io.IOException;
import net.minecraft.stats.ServerStatsCounter;
import org.apache.commons.io.FileUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Player stats: the text is made here, writing it to disk happens on another core. */
@Mixin(ServerStatsCounter.class)
public abstract class ServerStatsCounterMixin {
    @SuppressWarnings("deprecation")
    @Redirect(method = "save", at = @At(value = "INVOKE", target = "Lorg/apache/commons/io/FileUtils;writeStringToFile(Ljava/io/File;Ljava/lang/String;)V", remap = false))
    private void charboost$write(File file, String text) throws IOException {
        if (AsyncSaves.live()) {
            AsyncSaves.textFile(file, text);
        } else {
            FileUtils.writeStringToFile(file, text);
        }
    }
}
