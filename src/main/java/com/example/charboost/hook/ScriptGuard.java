package com.example.charboost.hook;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;

/** Is this mod's script file byte for byte the one a shortcut was written against? If not, the script itself keeps running. */
public final class ScriptGuard {
    private static final Map<String, String> HASHES = new HashMap<>();

    private ScriptGuard() {
    }

    public static boolean same(String mod, String file, String sha1) {
        return sha1.equals(HASHES.computeIfAbsent(mod + "/" + file, k -> hash(mod, file)));
    }

    private static String hash(String mod, String file) {
        try {
            IModFileInfo info = ModList.get().getModFileById(mod);
            if (info != null) {
                Path path = info.getFile().findResource(file);
                if (path != null && Files.exists(path)) {
                    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path)));
                }
            }
        } catch (Exception e) {
            return "unreadable";
        }
        return "missing";
    }
}
