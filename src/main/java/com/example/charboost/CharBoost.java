package com.example.charboost;

import com.example.charboost.hook.Scripts;
import com.example.charboost.mixin.MixinGate;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Boost.MODID)
public class CharBoost {
    public static final Logger LOG = LoggerFactory.getLogger("CharBoost");

    public CharBoost() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Cfg.SPEC, "charboost.toml");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfig);
        MinecraftForge.EVENT_BUS.register(new Watcher());
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> BoostCommand.register(e.getDispatcher()));
    }

    private void onConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() == Cfg.SPEC) {
            Cfg.apply();
            Scripts.refreshLists();
        }
    }

    /** One line in the log on start saying which hooks really went in. */
    static void reportHooks() {
        LOG.info("[CharBoost] {} hooks in: {}", MixinGate.APPLIED.size(), String.join(", ", MixinGate.APPLIED));
    }
}
