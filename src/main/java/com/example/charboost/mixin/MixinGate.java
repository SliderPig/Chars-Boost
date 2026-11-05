package com.example.charboost.mixin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Decides which hooks go in at all. config/charboost.toml can turn off the whole mod or any one part
 * before anything is patched, so a clash with another mod can always be switched off without a crash loop.
 * Also skips the Palladium and KubeJS hooks when those mods aren't there, and logs what went in.
 */
public class MixinGate implements IMixinConfigPlugin {
    private static final Logger LOG = LogManager.getLogger("CharBoost");
    public static final List<String> APPLIED = new CopyOnWriteArrayList<>();

    /** Which switch each hook belongs to. A hook with several goes in if any of them is on. */
    private static final Map<String, String[]> SWITCHES = new HashMap<>();

    static {
        put("EntityLookupMixin", "selectorIndex", "selectorTags");
        put("EntityTagsMixin", "selectorTags");
        put("EntitySelectorMixin", "selectorTags");
        put("EntitySelectorParserMixin", "selectorTags");
        put("MobMixin", "mobAi");
        put("BrainMixin", "mobAi");
        put("LivingEntityMixin", "mobPush");
        put("PathNavigationMixin", "pathLimits");
        put("MobDespawnMixin", "despawnChecks");
        put("HopperMixin", "hoppers");
        put("PlayerDataStorageMixin", "asyncSaves");
        put("PlayerAdvancementsMixin", "asyncSaves");
        put("ServerStatsCounterMixin", "asyncSaves");
        put("SavedDataMixin", "asyncSaves");
        put("palladium.PowerHandlerMixin", "powerProviders");
        put("palladium.PowerUtilMixin", "powerProviders");
        put("palladium.PropertyManagerMixin", "palladiumProperties");
        put("palladium.RegisterPropertyEventMixin", "palladiumProperties");
        put("palladium.KubeJSPluginMixin", "palladiumProperties");
        put("palladium.AbilityInstanceMixin", "palladiumAbilities", "afkPlayers");
        put("palladium.AbilityUtilMixin", "palladiumAbilities");
        put("palladium.DefaultPowerHolderMixin", "palladiumAbilities", "afkPlayers");
        put("kubejs.KubeJSPlayerEventHandlerMixin", "afkPlayers");
    }

    private static void put(String mixin, String... switches) {
        SWITCHES.put(mixin, switches);
    }

    private Map<String, String> file = Map.of();
    private String rootPackage = "";

    @Override
    public void onLoad(String mixinPackage) {
        rootPackage = "com.example.charboost.mixin.";
        file = readConfig();
    }

    /** The config system isn't up this early, so this reads the "name = false" lines itself. */
    private static Map<String, String> readConfig() {
        Map<String, String> out = new HashMap<>();
        Path path = FMLPaths.CONFIGDIR.get().resolve("charboost.toml");
        if (!Files.exists(path)) {
            return out;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                String l = line.trim();
                int eq = l.indexOf('=');
                if (eq > 0 && !l.startsWith("#")) {
                    out.put(l.substring(0, eq).trim(), l.substring(eq + 1).trim());
                }
            }
        } catch (IOException e) {
            LOG.warn("Couldn't read config/charboost.toml, using defaults");
        }
        return out;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String name = mixinClassName.startsWith(rootPackage) ? mixinClassName.substring(rootPackage.length()) : mixinClassName;
        if ("false".equals(file.get("enabled"))) {
            return false;
        }
        LoadingModList mods = LoadingModList.get();
        if (name.startsWith("palladium.") && (mods == null || mods.getModFileById("palladium") == null)) {
            return false;
        }
        if ((name.startsWith("kubejs.") || name.equals("palladium.KubeJSPluginMixin") || name.equals("palladium.RegisterPropertyEventMixin")
                || name.startsWith("palladium.Scriptable"))
                && (mods == null || mods.getModFileById("kubejs") == null)) {
            return false;
        }
        String[] switches = SWITCHES.get(name);
        if (switches == null) {
            return true;
        }
        for (String s : switches) {
            if (!"false".equals(file.get(s))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        String simple = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        APPLIED.add(simple);
        LOG.info("[CharBoost] hooked {} into {}", simple, targetClassName);
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return new ArrayList<>();
    }
}
