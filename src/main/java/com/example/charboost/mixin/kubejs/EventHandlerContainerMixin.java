package com.example.charboost.mixin.kubejs;

import com.example.charboost.hook.ScriptEntryHolder;
import com.example.charboost.hook.Scripts;
import dev.latvian.mods.kubejs.event.EventExceptionHandler;
import dev.latvian.mods.kubejs.event.EventExit;
import dev.latvian.mods.kubejs.event.EventHandlerContainer;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.event.IEventHandler;
import dev.latvian.mods.rhino.WrappedException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/** Runs a KubeJS event's handlers the same way as always, with a stopwatch round each one. */
@Mixin(value = EventHandlerContainer.class, remap = false)
public abstract class EventHandlerContainerMixin implements ScriptEntryHolder {
    @Shadow
    @Final
    public IEventHandler handler;
    @Shadow
    @Final
    public String source;
    @Shadow
    @Final
    public int line;
    @Shadow
    EventHandlerContainer child;

    @Unique
    private Scripts.Entry charboost$entry;
    @Unique
    private boolean charboost$marked;

    @Unique
    private Scripts.Entry charboost$entry() {
        Scripts.Entry e = charboost$entry;
        if (e == null) {
            e = Scripts.entry(source, line);
            charboost$entry = e;
        }
        return e;
    }

    @Override
    public void charboost$markTick() {
        if (charboost$marked) {
            return;
        }
        charboost$marked = true;
        charboost$entry().tickEvent = true;
        if (child != null) {
            ((ScriptEntryHolder) child).charboost$markTick();
        }
    }

    /**
     * @author Char
     * @reason The same loop as KubeJS's own, timing each handler. Slow-listed tick handlers can sit a tick out.
     */
    @Overwrite
    public EventResult handle(EventJS event, EventExceptionHandler exh) throws EventExit {
        EventHandlerContainerMixin itr = this;
        do {
            Scripts.Entry entry = itr.charboost$entry();
            if (!Scripts.skip(entry)) {
                long start = System.nanoTime();
                try {
                    itr.handler.onEvent(event);
                } catch (EventExit exit) {
                    throw exit;
                } catch (Throwable thrown) {
                    Throwable throwable = thrown;
                    while (throwable instanceof WrappedException wrapped) {
                        throwable = wrapped.getWrappedException();
                    }
                    if (throwable instanceof EventExit exit) {
                        throw exit;
                    }
                    if (exh == null || (throwable = exh.handle(event, (EventHandlerContainer) (Object) itr, throwable)) != null) {
                        throw EventResult.Type.ERROR.exit(throwable);
                    }
                } finally {
                    entry.cost.add(System.nanoTime() - start);
                }
            }
            itr = (EventHandlerContainerMixin) (Object) itr.child;
        } while (itr != null);
        return EventResult.PASS;
    }
}
