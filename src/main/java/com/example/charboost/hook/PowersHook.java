package com.example.charboost.hook;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.fml.loading.LoadingModList;
import net.threetag.palladium.entity.PalladiumLivingEntityExtension;

/** Everything else talks to Palladium through here, so the mod still loads without Palladium. */
public final class PowersHook {
    private static final boolean HAS_PALLADIUM = LoadingModList.get() != null && LoadingModList.get().getModFileById("palladium") != null;
    /** Goes up when the data packs reload, so Palladium looks again for what everybody should have. */
    public static int generation = 0;

    private PowersHook() {
    }

    public static boolean hasPalladium() {
        return HAS_PALLADIUM;
    }

    public static boolean holdsPowers(LivingEntity entity) {
        return HAS_PALLADIUM && Inner.holds(entity);
    }

    /** For /charboost mobs: which powers mobs are holding, most common first. */
    public static String powersOnMobs(MinecraftServer server) {
        return HAS_PALLADIUM ? Inner.powersOnMobs(server) : "none";
    }

    private static final class Inner {
        static String powersOnMobs(MinecraftServer server) {
            Map<String, Integer> counts = new HashMap<>();
            for (ServerLevel level : server.getAllLevels()) {
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof Mob && entity instanceof PalladiumLivingEntityExtension ext) {
                        for (var id : ext.palladium$getPowerHandler().getPowerHolders().keySet()) {
                            counts.merge(id.toString(), 1, Integer::sum);
                        }
                    }
                }
            }
            return counts.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(10)
                    .map(e -> e.getKey() + " x" + e.getValue()).toList().toString();
        }

        static boolean holds(LivingEntity entity) {
            return entity instanceof PalladiumLivingEntityExtension ext && !ext.palladium$getPowerHandler().getPowerHolders().isEmpty();
        }
    }
}
