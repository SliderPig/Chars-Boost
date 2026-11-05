package com.example.charboost.mixin;

import com.example.charboost.hook.AsyncSaves;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Advancements: the JSON tree is built here as always, turning it into text and writing it happens on another core. */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow
    @Final
    private static Gson GSON;
    @Shadow
    @Final
    private Path playerSavePath;
    @Shadow
    @Final
    private Map<Advancement, AdvancementProgress> progress;

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void charboost$save(CallbackInfo ci) {
        if (!AsyncSaves.live()) {
            return;
        }
        Map<ResourceLocation, AdvancementProgress> map = new LinkedHashMap<>();
        for (Map.Entry<Advancement, AdvancementProgress> entry : progress.entrySet()) {
            if (entry.getValue().hasProgress()) {
                map.put(entry.getKey().getId(), entry.getValue());
            }
        }
        JsonElement json = GSON.toJsonTree(map);
        json.getAsJsonObject().addProperty("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        AsyncSaves.jsonFile(GSON, json, playerSavePath);
        ci.cancel();
    }
}
