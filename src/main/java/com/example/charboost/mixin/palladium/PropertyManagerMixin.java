package com.example.charboost.mixin.palladium;

import com.example.charboost.Opt;
import java.util.HashMap;
import java.util.Map;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * palladium.getProperty and setProperty look a property up by name, and that went through every property the entity has
 * one by one. A player has hundreds, and scripts and conditions do this thousands of times a tick. A name lookup gives
 * the same answer straight away. Built the first time it's needed, so entities nobody asks about pay nothing.
 */
@Mixin(value = PropertyManager.class, remap = false)
public abstract class PropertyManagerMixin {
    @Shadow
    @Final
    Map<PalladiumProperty<?>, Object> values;

    @Unique
    private HashMap<String, PalladiumProperty<?>> charboost$byName;
    @Unique
    private int charboost$indexed;

    @Inject(method = "getPropertyByName", at = @At("HEAD"), cancellable = true)
    private void charboost$lookup(String name, CallbackInfoReturnable<PalladiumProperty<?>> cir) {
        int size = values.size();
        if (size < 8 || !Opt.PROPERTIES.live()) {
            return;
        }
        Opt.PROPERTIES.seen++;
        HashMap<String, PalladiumProperty<?>> index = charboost$byName;
        if (index == null || charboost$indexed != size) {
            index = new HashMap<>(size * 2);
            // putIfAbsent keeps the first of any two with the same name, like the original did.
            for (PalladiumProperty<?> property : values.keySet()) {
                index.putIfAbsent(property.getKey(), property);
            }
            charboost$byName = index;
            charboost$indexed = size;
        }
        Opt.PROPERTIES.skips++;
        Opt.PROPERTIES.savedNs += 10L * size;
        cir.setReturnValue(index.get(name));
    }
}
