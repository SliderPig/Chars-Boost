package com.example.charboost.mixin.palladium;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.PowerManager;
import net.threetag.palladium.power.PowerUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * "Does this entity have this power?" went through every power one by one and made several objects each time, for every
 * entity in every selector like @e[palladium.power=x], and for every script that asks. A direct lookup gives the same answer.
 */
@Mixin(value = PowerUtil.class, remap = false)
public abstract class PowerUtilMixin {
    /**
     * @author Char
     * @reason Same answer, without the allocations. Called far too often.
     */
    @Overwrite
    public static boolean hasPower(LivingEntity entity, ResourceLocation powerId) {
        var handler = PowerManager.getPowerHandler(entity);
        return handler.isPresent() && handler.get().getPowerHolders().containsKey(powerId);
    }
}
