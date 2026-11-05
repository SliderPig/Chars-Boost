package com.example.charboost.hook;

import com.example.charboost.CharBoost;
import com.example.charboost.Opt;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

/**
 * Writes files on its own thread so the main thread isn't held up by compressing and disk waits.
 * The data is copied on the main thread first, so nothing on this thread ever touches the live world.
 * One worker, first in first out, so a newer save of the same file can't be overtaken by an older one.
 * The server waits for it to finish when it stops.
 */
public final class AsyncSaves {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Char's Boost saves");
        t.setDaemon(true);
        return t;
    });
    private static final Map<String, Future<?>> LAST_FOR = new ConcurrentHashMap<>();
    private static final AtomicLong PENDING = new AtomicLong();
    public static final AtomicLong FILES = new AtomicLong();
    public static final AtomicLong OFF_MAIN_NANOS = new AtomicLong();

    private AsyncSaves() {
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> flush(30), "Char's Boost saves flush"));
    }

    public static boolean live() {
        return Opt.SAVES.live();
    }

    private static void submit(String key, IoJob job) {
        PENDING.incrementAndGet();
        Opt.SAVES.skips++;
        Future<?> f = WORKER.submit(() -> {
            long start = System.nanoTime();
            try {
                job.run();
            } catch (Throwable e) {
                CharBoost.LOG.error("Couldn't save {}", key, e);
            } finally {
                long took = System.nanoTime() - start;
                OFF_MAIN_NANOS.addAndGet(took);
                FILES.incrementAndGet();
                PENDING.decrementAndGet();
            }
        });
        LAST_FOR.put(key, f);
    }

    /** A player's main file. Same steps as the game does, just not on the main thread. */
    public static void playerFile(File dir, String uuid, CompoundTag data) {
        CompoundTag snapshot = data.copy();
        submit(uuid, () -> {
            File temp = File.createTempFile(uuid + "-", ".dat", dir);
            NbtIo.writeCompressed(snapshot, temp);
            Util.safeReplaceFile(new File(dir, uuid + ".dat"), temp, new File(dir, uuid + ".dat_old"));
        });
    }

    /** Waits until the player's file is really on disk before it gets read back. */
    public static void waitForPlayer(String uuid) {
        Future<?> f = LAST_FOR.remove(uuid);
        if (f != null) {
            try {
                f.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                CharBoost.LOG.warn("Timed out waiting to save {}", uuid);
            }
        }
    }

    /** Any NBT file (world saved data). Written beside the real file then moved over it, so a crash can't leave half a file. */
    public static void nbtFile(CompoundTag data, File file) {
        CompoundTag snapshot = data.copy();
        submit(file.getPath(), () -> {
            File temp = new File(file.getParentFile(), file.getName() + ".charboost");
            NbtIo.writeCompressed(snapshot, temp);
            move(temp.toPath(), file.toPath());
        });
    }

    public static void jsonFile(Gson gson, JsonElement json, Path path) {
        submit(path.toString(), () -> {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(path.getFileName() + ".charboost");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                gson.toJson(json, writer);
            }
            move(temp, path);
        });
    }

    public static void textFile(File file, String text) {
        submit(file.getPath(), () -> {
            Path path = file.toPath();
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Path temp = path.resolveSibling(path.getFileName() + ".charboost");
            Files.writeString(temp, text, StandardCharsets.UTF_8);
            move(temp, path);
        });
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static long pending() {
        return PENDING.get();
    }

    /** Blocks until everything queued so far is written. */
    public static void flush(int seconds) {
        try {
            WORKER.submit(() -> {
            }).get(seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            CharBoost.LOG.warn("Saves still writing after {} seconds", seconds);
        }
        LAST_FOR.clear();
    }

    private interface IoJob {
        void run() throws IOException;
    }
}
