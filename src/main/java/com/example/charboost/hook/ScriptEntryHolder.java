package com.example.charboost.hook;

import dev.latvian.mods.kubejs.event.EventHandlerContainer;

/** Added to KubeJS's event handler chain. Only ever touched from code that runs when KubeJS is there. */
public interface ScriptEntryHolder {
    Scripts.Entry charboost$entry();

    EventHandlerContainer charboost$next();

    /** Marks this handler and all that follow it as belonging to a tick event. */
    void charboost$markTick();
}
