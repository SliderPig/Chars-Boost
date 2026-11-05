package com.example.charboost.mixin.palladium;

import com.example.charboost.hook.PropertySync;
import net.threetag.palladium.util.property.EntityPropertyHandler;
import net.threetag.palladium.util.property.PalladiumProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** See PropertySync. */
@Mixin(value = EntityPropertyHandler.class, remap = false)
public abstract class EntityPropertyHandlerMixin {
    @Inject(method = "onChanged", at = @At("HEAD"), cancellable = true)
    private <T> void charboost$hold(PalladiumProperty<T> property, T oldValue, T newValue, CallbackInfo ci) {
        if (PropertySync.hold((EntityPropertyHandler) (Object) this, property, oldValue, newValue)) {
            ci.cancel();
        }
    }
}
