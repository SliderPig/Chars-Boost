package com.example.charboost.hook;

/** Added to KubeJS's event handler. */
public interface EventHandlerHeads {
    /** The first server script and the first startup script listening to this event. A reload changes them. */
    Object[] charboost$heads();
}
