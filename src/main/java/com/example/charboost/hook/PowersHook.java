package com.example.charboost.hook;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.loading.LoadingModList;
import net.threetag.palladium.entity.PalladiumLivingEntityExtension;

/** Everything else talks to Palladium through here, so the mod still loads without Palladium. */
public final class PowersHook {
    private static final boolean HAS_PALLADIUM = LoadingModList.get() != null && LoadingModList.get().getModFileById("palladium") != null;
    /** Goes up when the data packs reload, so Palladium looks again for what everybody should have. */
    public static int generation = 0;

    private PowersHook() {
    }

    public static boolean holdsPowers(LivingEntity entity) {
        return HAS_PALLADIUM && Inner.holds(entity);
    }

    private static final class Inner {
        static boolean holds(LivingEntity entity) {
            return entity instanceof PalladiumLivingEntityExtension ext && !ext.palladium$getPowerHandler().getPowerHolders().isEmpty();
        }
    }
}
