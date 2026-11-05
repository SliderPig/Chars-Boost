package com.example.charboost.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Counts how many loaded entities have each tag. Only ever asked "could anybody have this tag?".
 * A count that is too high only means a normal search runs. It must never be too low, which is why every
 * way a tag can change goes through {@code TrackedTags}.
 */
public final class TagCensus {
    private final Map<String, int[]> counts = new HashMap<>();

    public void add(String tag) {
        int[] c = counts.get(tag);
        if (c == null) {
            counts.put(tag, new int[]{1});
        } else {
            c[0]++;
        }
    }

    public void remove(String tag) {
        int[] c = counts.get(tag);
        if (c != null && --c[0] <= 0) {
            counts.remove(tag);
        }
    }

    public boolean anyHave(String tag) {
        return counts.containsKey(tag);
    }

    /** True if every one of these tags is held by somebody. */
    public boolean anyHaveAll(String[] tags) {
        for (String tag : tags) {
            if (!counts.containsKey(tag)) {
                return false;
            }
        }
        return true;
    }

    public int count(String tag) {
        int[] c = counts.get(tag);
        return c == null ? 0 : c[0];
    }

    public int size() {
        return counts.size();
    }
}
