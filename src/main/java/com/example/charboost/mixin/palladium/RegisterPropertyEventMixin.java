package com.example.charboost.mixin.palladium;

import com.example.charboost.hook.PropertyCache;
import com.example.charboost.hook.RegisterEventAccess;
import net.minecraft.world.entity.Entity;
import net.threetag.palladium.compat.kubejs.RegisterPalladiumPropertyEventJS;
import net.threetag.palladium.util.property.EntityPropertyHandler;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Lets PropertyCache see what the scripts register. */
@Mixin(value = RegisterPalladiumPropertyEventJS.class, remap = false)
public abstract class RegisterPropertyEventMixin implements RegisterEventAccess {
    @Shadow
    @Final
    private Entity entity;
    @Shadow
    @Final
    private EntityPropertyHandler handler;

    @Override
    public Entity charboost$entity() {
        return entity;
    }

    @Override
    public EntityPropertyHandler charboost$handler() {
        return handler;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Redirect(method = "registerProperty", at = @At(value = "INVOKE", target = "Lnet/threetag/palladium/util/property/EntityPropertyHandler;register(Lnet/threetag/palladium/util/property/PalladiumProperty;Ljava/lang/Object;)Lnet/threetag/palladium/util/property/PropertyManager;"))
    private PropertyManager charboost$note(EntityPropertyHandler target, PalladiumProperty property, Object value) {
        PropertyCache.note(property, value);
        return target.register(property, value);
    }
}
