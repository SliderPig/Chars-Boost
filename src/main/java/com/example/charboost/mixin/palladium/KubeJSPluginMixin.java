package com.example.charboost.mixin.palladium;

import com.example.charboost.hook.PropertyCache;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.event.EventResult;
import net.threetag.palladium.compat.kubejs.PalladiumKubeJSPlugin;
import net.threetag.palladium.compat.kubejs.RegisterPalladiumPropertyEventJS;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Where a new entity asks the scripts for its properties. See PropertyCache. */
@Mixin(value = PalladiumKubeJSPlugin.class, remap = false)
public abstract class KubeJSPluginMixin {
    @Redirect(method = "lambda$init$1", at = @At(value = "INVOKE", target = "Ldev/latvian/mods/kubejs/event/EventHandler;post(Ldev/latvian/mods/kubejs/event/EventJS;)Ldev/latvian/mods/kubejs/event/EventResult;"))
    private static EventResult charboost$post(EventHandler handler, EventJS event) {
        if (event instanceof RegisterPalladiumPropertyEventJS register) {
            return PropertyCache.post(handler, register);
        }
        return handler.post(event);
    }
}
