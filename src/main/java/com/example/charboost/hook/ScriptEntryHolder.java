package com.example.charboost.hook;

/** Added to KubeJS's event handler chain. */
public interface ScriptEntryHolder {
    /** Marks this handler and all that follow it as belonging to a tick event. */
    void charboost$markTick();
}
