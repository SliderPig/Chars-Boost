package com.example.charboost.hook;

import com.example.charboost.Boost;
import com.example.charboost.CharBoost;
import dev.latvian.mods.kubejs.player.PlayerEventJS;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.AbilityUtil;
import net.threetag.palladium.util.property.EntityPropertyHandler;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * The few script handlers that were by far the worst on the Powers server (timed with /charboost scripts), done in Java with the
 * same result. A replacement only goes in if the script file is byte for byte the one this was written against, so if a
 * mod updates the script, the mod's own script just runs like normal.
 */
public final class NativeScripts {
    private record Spec(String mod, String file, String sha1, Consumer<Object> code, long nanosEach, boolean perEntity) {
    }

    private static final Map<String, Spec> SPECS = new HashMap<>();

    static {
        SPECS.put("addonpack_eop:kubejs_scripts/property.js:25", new Spec("eop", "data/eop/kubejs_scripts/property.js",
                "7f68f2a7779e2b3fbcbc66a9545c7fb9df0f9fc0", NativeScripts::echoesTagProperties, 17_000, true));
        SPECS.put("addonpack_fsang_hero:kubejs_scripts/elytra_flight.js:1", new Spec("fsang", "data/fsang_hero/kubejs_scripts/elytra_flight.js",
                "b834dee0cead2746efe9a76a58e2ec0219bbe62a", NativeScripts::fsangElytraFlight, 1_500_000, false));
        SPECS.put("addonpack_fsang_hero:kubejs_scripts/properties.js:1307", new Spec("fsang", "data/fsang_hero/kubejs_scripts/properties.js",
                "430376705c9743afcc6442086ee7dac31aedbad9", NativeScripts::nothing, 3_400, true));
    }

    private NativeScripts() {
    }

    /** The replacement for this script handler, or null if it has none or the script has changed. */
    public static Consumer<Object> find(String name) {
        Spec spec = SPECS.get(name);
        if (spec == null) {
            return null;
        }
        try {
            IModFileInfo info = ModList.get().getModFileById(spec.mod);
            if (info != null) {
                Path path = info.getFile().findResource(spec.file);
                if (path != null && Files.exists(path)) {
                    String sha1 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path)));
                    if (sha1.equals(spec.sha1)) {
                        CharBoost.LOG.info("[CharBoost] {} now runs in Java", name);
                        return spec.code;
                    }
                    CharBoost.LOG.info("[CharBoost] {} has changed, leaving it as a script", name);
                }
            }
        } catch (Exception e) {
            CharBoost.LOG.warn("[CharBoost] Couldn't check {}: {}", name, e.toString());
        }
        return null;
    }

    /** What the script would have cost this time, roughly. Measured on the Powers mods. */
    public static long estimate(String name) {
        Spec spec = SPECS.get(name);
        if (spec == null) {
            return 0;
        }
        return spec.perEntity ? spec.nanosEach * Census.loaded : spec.nanosEach;
    }

    // ------- Echoes property.js:25. Every tick, for every living entity on the server: 7 properties set to "has this tag".

    private static final String[] TAGS = {"EOP.inWater", "EOP.inDark", "Phero.Target", "EOP.Warden.Sense", "EOP.LivingEntities", "EOP.onGround", "EOP.Cursed.Tag"};
    private static final String[] PROPERTIES = {"EOP.inWater", "EOP.inDark", "EOP.Tracked", "EOP.Warden.Sense", "EOP.LivingEntities", "EOP.onGround", "EOP.Cursed.Tag"};
    /** Entities with at least one of these properties on. */
    private static final Set<Entity> HOLDERS = new ReferenceOpenHashSet<>();
    private static long lastSweep = 0;

    private static void echoesTagProperties(Object event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        // Once in a while look at everybody, to catch anything that came in with its properties already on.
        boolean sweep = Boost.tick - lastSweep >= 100;
        if (sweep) {
            lastSweep = Boost.tick;
            HOLDERS.removeIf(Entity::isRemoved);
        }
        boolean anyTag = !Census.working();
        for (int i = 0; i < TAGS.length && !anyTag; i++) {
            anyTag = Boost.CENSUS.anyHave(TAGS[i]);
        }
        if (anyTag || sweep) {
            for (ServerLevel level : server.getAllLevels()) {
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof LivingEntity) {
                        echoesOne(entity, sweep);
                    }
                }
            }
        } else if (!HOLDERS.isEmpty()) {
            for (Entity entity : HOLDERS.toArray(new Entity[0])) {
                echoesOne(entity, false);
            }
        }
    }

    private static void echoesOne(Entity entity, boolean sweep) {
        Set<String> tags = entity.getTags();
        boolean wants = false;
        if (!tags.isEmpty()) {
            for (String tag : TAGS) {
                if (tags.contains(tag)) {
                    wants = true;
                    break;
                }
            }
        }
        if (!wants && !sweep && !HOLDERS.contains(entity)) {
            return;
        }
        var handler = EntityPropertyHandler.getHandler(entity).orElse(null);
        if (handler == null) {
            return;
        }
        boolean anyOn = false;
        for (int i = 0; i < TAGS.length; i++) {
            boolean on = tags.contains(TAGS[i]);
            anyOn |= on;
            set(handler, PROPERTIES[i], on);
        }
        if (anyOn) {
            HOLDERS.add(entity);
        } else {
            HOLDERS.remove(entity);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void set(EntityPropertyHandler handler, String name, boolean on) {
        PalladiumProperty property = handler.getPropertyByName(name);
        if (property != null) {
            Object now = handler.get(property);
            // The original sets it every tick. Setting the same value again does nothing for a yes/no property anyway.
            if (!(now instanceof Boolean b) || b != on) {
                handler.set(property, Boolean.valueOf(on));
            }
        }
    }

    // ------- FSang elytra_flight.js:1. Every tick, for every player, went through every ability looking for this one type.

    private static Ability elytra;
    private static boolean elytraLooked;

    private static void fsangElytraFlight(Object event) {
        if (!(event instanceof PlayerEventJS playerEvent)) {
            return;
        }
        if (!elytraLooked) {
            elytraLooked = true;
            elytra = Ability.REGISTRY.get(new ResourceLocation("fsang", "elytra_flight"));
        }
        Player player = playerEvent.getPlayer();
        if (elytra == null || player == null || player.onGround()) {
            return;
        }
        for (AbilityInstance ignored : AbilityUtil.getEnabledInstances(player, elytra)) {
            player.startFallFlying();
        }
    }

    // ------- FSang properties.js:1307. Every 4 ticks, goes through every non-player entity on the server asking if it has
    // the property fsang_geo_sense. FSang only ever gives that property to players, so the answer is always no.

    private static void nothing(Object event) {
    }
}
