package com.example.charboost.mixin.palladium;

import com.example.charboost.Boost;
import com.example.charboost.Opt;
import com.example.charboost.hook.PowersHook;
import com.example.charboost.model.RollingCost;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.threetag.palladium.power.IPowerHandler;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.PowerCollector;
import net.threetag.palladium.power.PowerHandler;
import net.threetag.palladium.power.provider.EquipmentSlotPowerProvider;
import net.threetag.palladium.power.provider.PowerProvider;
import net.threetag.palladium.power.provider.SuitSetPowerProvider;
import net.threetag.palladium.power.provider.SuperpowerProvider;
import net.threetag.palladium.util.property.PalladiumProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Every tick, for every living thing, Palladium works out afresh which powers it should have: every slot, every Curios slot,
 * and every suit set in every mod (FSang alone adds 280). The answer only depends on what's worn and the superpower
 * list, so this looks at those first and only does the search when one of them changed. A slow check every couple of
 * seconds catches anything else. Powers being taken away isn't affected: Palladium checks that separately.
 */
@Mixin(value = PowerHandler.class, remap = false)
public abstract class PowerHandlerMixin {
    @Unique
    private static final EquipmentSlot[] charboost$SLOTS = EquipmentSlot.values();
    @Unique
    private static final RollingCost charboost$cost = new RollingCost();

    @Shadow
    @Final
    private LivingEntity entity;
    @Shadow
    @Final
    private List<IPowerHolder> tickRemoveBuffer;

    @Unique
    private final Item[] charboost$worn = new Item[charboost$SLOTS.length];
    @Unique
    private int charboost$idsHash;
    @Unique
    private int charboost$generation = -1;
    @Unique
    private long charboost$nextFull;
    @Unique
    private long charboost$decidedAt = -1;
    @Unique
    private boolean charboost$search;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/threetag/palladium/power/provider/PowerProvider;providePowers(Lnet/minecraft/world/entity/LivingEntity;Lnet/threetag/palladium/power/IPowerHandler;Lnet/threetag/palladium/power/PowerCollector;)V"))
    private void charboost$provide(PowerProvider provider, LivingEntity living, IPowerHandler handler, PowerCollector collector) {
        Opt.POWER_PROVIDERS.seen++;
        if (!Opt.POWER_PROVIDERS.live()) {
            provider.providePowers(living, handler, collector);
            return;
        }
        // A mod's own provider could depend on anything, so those always run.
        boolean known = provider instanceof SuitSetPowerProvider || provider instanceof EquipmentSlotPowerProvider
                || provider instanceof SuperpowerProvider || provider.getClass().getName().endsWith(".CuriosPowerProvider");
        if (!known) {
            provider.providePowers(living, handler, collector);
            return;
        }
        if (charboost$decidedAt != Boost.tick) {
            charboost$decidedAt = Boost.tick;
            charboost$search = charboost$changed();
        }
        if (charboost$search) {
            long start = System.nanoTime();
            provider.providePowers(living, handler, collector);
            charboost$cost.add(System.nanoTime() - start);
        } else {
            Opt.POWER_PROVIDERS.skips++;
            Opt.POWER_PROVIDERS.savedNs += (long) charboost$cost.average();
        }
    }

    @Unique
    private boolean charboost$changed() {
        LivingEntity self = entity;
        boolean player = self instanceof Player;
        long now = Boost.tick;
        boolean timeUp = now >= charboost$nextFull;
        // Other mobs only look now and then. A mob doesn't need its new armour's power this very tick.
        if (!player && !timeUp && tickRemoveBuffer.isEmpty() && (now + self.getId()) % 10 != 0) {
            return false;
        }
        boolean changed = timeUp || !tickRemoveBuffer.isEmpty() || charboost$generation != PowersHook.generation;
        Item[] worn = charboost$worn;
        for (int i = 0; i < worn.length; i++) {
            Item item = self.getItemBySlot(charboost$SLOTS[i]).getItem();
            if (item != worn[i]) {
                worn[i] = item;
                changed = true;
            }
        }
        int ids = PalladiumProperties.SUPERPOWER_IDS.get(self).hashCode();
        if (ids != charboost$idsHash) {
            charboost$idsHash = ids;
            changed = true;
        }
        if (changed) {
            charboost$generation = PowersHook.generation;
            // Curios slots aren't compared above, so players get a fresh search twice a second, mobs less.
            charboost$nextFull = now + (player ? 10 : 100 + (self.getId() & 31));
        }
        return changed;
    }
}
