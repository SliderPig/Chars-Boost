package com.example.charboost.hook;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Two scripted conditions that loop over a lot of things in script, done in Java with the same result. Each only goes in
 * if its script is byte for byte the one this was written against.
 */
public final class NativeConditions {
    private record Spec(String mod, String file, String sha1, BiPredicate<LivingEntity, Map<String, Object>> code) {
    }

    private static final Map<String, Spec> SPECS = new HashMap<>();

    static {
        SPECS.put("fsang:block_search", new Spec("fsang", "addon/fsang_hero/kubejs_scripts/block_condition.js",
                "2763124c7bb799fbbfba37a1ab79027763c0ffa3", NativeConditions::blockSearch));
        SPECS.put("fsang:inventory_item_check", new Spec("fsang", "addon/fsang_hero/kubejs_scripts/iventory_condition.js",
                "cd06c2773400e9970c04af5f7242a5d734244520", NativeConditions::inventoryItem));
    }

    private NativeConditions() {
    }

    public static BiPredicate<LivingEntity, Map<String, Object>> find(String condition) {
        Spec spec = SPECS.get(condition);
        return spec != null && ScriptGuard.same(spec.mod, spec.file, spec.sha1) ? spec.code : null;
    }

    /** Can the Java version answer this one? If not, the script runs. */
    public static boolean handles(String condition, LivingEntity entity, Map<String, Object> settings) {
        if (condition.equals("fsang:inventory_item_check")) {
            // Tags go by KubeJS's own idea of a tag, and only players have this inventory.
            return entity instanceof Player && settings.get("item_id") instanceof String id && !id.startsWith("#");
        }
        return settings.get("block_id") instanceof String && settings.get("search_radius") instanceof Number;
    }

    /** Any block of this kind inside a sphere round the entity, looking in the same order the script does. */
    private static boolean blockSearch(LivingEntity entity, Map<String, Object> settings) {
        String wanted = "block." + replaceFirstColon((String) settings.get("block_id"));
        double radius = ((Number) settings.get("search_radius")).doubleValue();
        double radiusSquared = radius * radius;
        Level level = entity.level();
        BlockPos at = entity.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (double x = -radius; x <= radius; x++) {
            for (double y = -radius; y <= radius; y++) {
                for (double z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSquared) {
                        continue;
                    }
                    pos.set(at.getX() + (int) x, at.getY() + (int) y, at.getZ() + (int) z);
                    if (level.getBlockState(pos).getBlock().getDescriptionId().equals(wanted)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean inventoryItem(LivingEntity entity, Map<String, Object> settings) {
        String wanted = "item." + replaceFirstColon((String) settings.get("item_id"));
        for (ItemStack stack : ((Player) entity).getInventory().items) {
            if (!stack.isEmpty() && stack.getItem().getDescriptionId().equals(wanted)) {
                return true;
            }
        }
        return false;
    }

    /** JavaScript's text.replace(':', '.') changes the first one only. */
    private static String replaceFirstColon(String text) {
        int at = text.indexOf(':');
        return at < 0 ? text : text.substring(0, at) + '.' + text.substring(at + 1);
    }
}
