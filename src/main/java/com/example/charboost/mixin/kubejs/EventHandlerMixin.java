package com.example.charboost.mixin.kubejs;

import com.example.charboost.hook.ScriptEntryHolder;
import dev.latvian.mods.kubejs.event.EventExceptionHandler;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventHandlerContainer;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.bindings.event.LevelEvents;
import dev.latvian.mods.kubejs.bindings.event.PlayerEvents;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.bindings.event.ServerEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks which handlers belong to tick events: only those may ever be slowed down. */
@Mixin(value = EventHandler.class, remap = false)
public abstract class EventHandlerMixin {
    @Inject(method = "postToHandlers", at = @At("HEAD"))
    private void charboost$mark(ScriptType type, EventHandlerContainer[] containers, EventJS event, EventExceptionHandler exh, CallbackInfo ci) {
        EventHandlerContainer head = containers[type.ordinal()];
        if (head == null) {
            return;
        }
        Object self = this;
        if (self == PlayerEvents.TICK || self == ServerEvents.TICK || self == LevelEvents.TICK) {
            ((ScriptEntryHolder) head).charboost$markTick();
        }
    }
}
