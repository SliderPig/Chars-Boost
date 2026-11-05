package com.example.charboost.mixin;

import com.example.charboost.Opt;
import com.example.charboost.hook.Census;
import com.example.charboost.hook.SelectorItems;
import com.example.charboost.hook.SelectorState;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @e[tag=x]: if nobody has tag x, there's nothing to find, so don't look through every entity.
 * @e[type=item,nbt={Item:{id:"x"}}]: if no dropped item is that kind, don't save them all to NBT to find that out.
 */
@Mixin(EntitySelector.class)
public abstract class EntitySelectorMixin implements SelectorState {
    @Unique
    private String[] charboost$tags = new String[0];
    @Unique
    private String charboost$item;

    @Override
    public void charboost$tags(String[] tags) {
        charboost$tags = tags;
    }

    @Override
    public void charboost$item(String item) {
        charboost$item = item;
    }

    @Inject(method = "addEntities", at = @At("HEAD"), cancellable = true)
    private void charboost$nobody(List<Entity> out, ServerLevel level, Vec3 position, Predicate<Entity> predicate, CallbackInfo ci) {
        Opt.SELECTOR_TAGS.seen++;
        if (Census.nobodyCouldMatch(charboost$tags)) {
            Opt.SELECTOR_TAGS.skips++;
            Opt.SELECTOR_TAGS.savedNs += (long) Census.loaded * Census.NANOS_PER_ENTITY / 3;
            ci.cancel();
        } else if (charboost$item != null && Opt.SELECTOR_TAGS.live() && SelectorItems.noneInLevel(level, charboost$item)) {
            Opt.SELECTOR_TAGS.skips++;
            ci.cancel();
        }
    }
}
