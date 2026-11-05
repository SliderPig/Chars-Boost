package com.example.charboost.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads what a selector like @e[type=x,tag=foo,tag=!bar] insists on. If nobody on the server has "foo", the selector can
 * only ever find nothing, so looking through every entity can be skipped. Anything it doesn't fully understand gives
 * no answer, which just means the normal search runs.
 */
public final class SelectorTags {
    private static final String[] NONE = new String[0];
    /** nbt={Item:{id:"x"...: a dropped item of that kind. Only when the id is the first thing written, as everybody writes it. */
    private static final Pattern ITEM_NBT = Pattern.compile("^\\{\\s*Item\\s*:\\s*\\{\\s*id\\s*:\\s*\"([^\"\\\\]+)\"");

    private SelectorTags() {
    }

    public static String[] required(String selector) {
        List<String> options = options(selector);
        if (options == null) {
            return NONE;
        }
        List<String> out = null;
        for (String option : options) {
            String tag = tagOf(option);
            if (tag != null) {
                if (out == null) {
                    out = new ArrayList<>(2);
                }
                if (!out.contains(tag)) {
                    out.add(tag);
                }
            }
        }
        return out == null ? NONE : out.toArray(NONE);
    }

    /**
     * For @e[type=item,nbt={Item:{id:"x"}}]: the item id it wants, or null if the selector isn't like that.
     * Those save the whole of every dropped item to NBT just to read its id.
     */
    public static String requiredItem(String selector) {
        List<String> options = options(selector);
        if (options == null) {
            return null;
        }
        boolean itemType = false;
        String id = null;
        for (String option : options) {
            int eq = option.indexOf('=');
            if (eq < 0) {
                return null;
            }
            String key = option.substring(0, eq).trim();
            String value = option.substring(eq + 1).trim();
            if (key.equals("type")) {
                if (value.equals("item") || value.equals("minecraft:item")) {
                    itemType = true;
                }
            } else if (key.equals("nbt") && id == null) {
                Matcher m = ITEM_NBT.matcher(value);
                if (m.find()) {
                    id = m.group(1);
                }
            }
        }
        return itemType ? id : null;
    }

    /** The options between the brackets, split at the top level only. Null if the text isn't one of the game's own selectors. */
    private static List<String> options(String selector) {
        if (selector == null || selector.length() < 4 || !selector.startsWith("@") || selector.charAt(2) != '[') {
            return null;
        }
        // Only the game's own selectors. A mod can add its own letters, and who knows what those do.
        if ("aeprs".indexOf(selector.charAt(1)) < 0 || !selector.endsWith("]")) {
            return null;
        }
        String body = selector.substring(3, selector.length() - 1);
        List<String> out = new ArrayList<>(4);
        int depth = 0;
        boolean quoted = false;
        char quote = 0;
        int start = 0;
        for (int i = 0; i <= body.length(); i++) {
            char c = i < body.length() ? body.charAt(i) : ',';
            if (quoted) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quoted = false;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                quoted = true;
                quote = c;
            } else if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
                if (depth < 0) {
                    return null;
                }
            } else if (c == ',' && depth == 0) {
                out.add(body.substring(start, i).trim());
                start = i + 1;
            }
        }
        return quoted || depth != 0 ? null : out;
    }

    private static String tagOf(String option) {
        int eq = option.indexOf('=');
        if (eq < 0 || !option.substring(0, eq).trim().equals("tag")) {
            return null;
        }
        String value = option.substring(eq + 1).trim();
        // tag=!x is "must not have x" and tag= is "has no tags": neither needs anybody to have a tag.
        if (value.isEmpty() || value.startsWith("!")) {
            return null;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean plain = c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                    || c == '_' || c == '-' || c == '.' || c == '+';
            if (!plain) {
                return null;
            }
        }
        return value;
    }
}
