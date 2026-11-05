package com.example.charboost;

import com.example.charboost.hook.AsyncSaves;
import com.example.charboost.hook.Functions;
import com.example.charboost.hook.PowersHook;
import com.example.charboost.hook.Scripts;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** The stopwatch round every server tick, and what feeds the tuner once a second. */
public final class Watcher {
    private long tickStart;
    private long secondStart = System.nanoTime();
    private long sumNanos;
    private int ticksThisSecond;
    private long lastWindowTick;
    private long lastSaveNanos;

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Boost.tick++;
            Functions.tickStart();
            tickStart = System.nanoTime();
            return;
        }
        long now = System.nanoTime();
        sumNanos += now - tickStart;
        ticksThisSecond++;
        if (now - secondStart >= 1_000_000_000L) {
            oneSecond(now);
        }
    }

    private void oneSecond(long now) {
        double seconds = (now - secondStart) / 1e9;
        double mspt = sumNanos / 1e6 / Math.max(1, ticksThisSecond);
        Boost.mspt = Boost.mspt == 0 ? mspt : Boost.mspt * 0.7 + mspt * 0.3;
        Boost.tps = Boost.tps * 0.7 + Math.min(20, ticksThisSecond / seconds) * 0.3;
        secondStart = now;
        sumNanos = 0;
        ticksThisSecond = 0;

        int level = Boost.tuner.feed(mspt);
        if (Boost.pinnedLevel >= 0) {
            level = Boost.pinnedLevel;
        }
        if (!Boost.enabled) {
            level = 0;
        }
        if (level != Boost.level) {
            CharBoost.LOG.debug("[CharBoost] pressure {} at {} ms", level, String.format("%.1f", mspt));
        }
        Boost.level = level;

        // The score for /charboost status: time saved per tick over the last few seconds.
        long ticks = Boost.tick - lastWindowTick;
        if (ticks >= 100) {
            for (Opt o : Opt.values()) {
                o.savedMsPerTick = (o.savedNs - o.windowStart) / 1e6 / ticks;
                o.windowStart = o.savedNs;
            }
            lastWindowTick = Boost.tick;
            // What the save thread did is real time the main thread didn't spend.
            long nanos = AsyncSaves.OFF_MAIN_NANOS.get();
            Opt.SAVES.savedNs += nanos - lastSaveNanos;
            lastSaveNanos = nanos;
        }
    }

    @SubscribeEvent
    public void onReload(OnDatapackSyncEvent event) {
        // getPlayer() is null when it's a reload for everybody, not somebody joining.
        if (event.getPlayer() == null) {
            PowersHook.generation++;
            Scripts.refreshLists();
        }
    }

    @SubscribeEvent
    public void onStarted(ServerStartedEvent event) {
        CharBoost.reportHooks();
    }

    @SubscribeEvent
    public void onStopped(ServerStoppedEvent event) {
        AsyncSaves.flush(60);
    }
}
