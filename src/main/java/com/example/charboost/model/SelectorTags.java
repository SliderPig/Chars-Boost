package com.example.charboost.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads which tags a selector like @e[type=x,tag=foo,tag=!bar] insists on. If nobody on the server has
 * "foo", the selector can only ever find nothing, so looking through every entity can be skipped.
 * Anything it doesn't fully understand gives no tags, which just means the normal search runs.
 */
public final class SelectorTags {
    private static final String[] NONE = new String[0];

    private SelectorTags() {
    }

    public static String[] required(String selector) {
        if (selector == null || selector.length() < 4 || !selector.startsWith("@") || selector.charAt(2) != '[') {
            return NONE;
        }
        // Only the game's own selectors. A mod can add its own letters, and who knows what those do.
        if ("aeprs".indexOf(selector.charAt(1)) < 0) {
            return NONE;
        }
        if (!selector.endsWith("]")) {
            return NONE;
        }
        String body = selector.substring(3, selector.length() - 1);
        List<String> out = null;
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
                    return NONE;
                }
            } else if (c == ',' && depth == 0) {
                String tag = tagOf(body.substring(start, i).trim());
                if (tag != null) {
                    if (out == null) {
                        out = new ArrayList<>(2);
                    }
                    if (!out.contains(tag)) {
                        out.add(tag);
                    }
                }
                start = i + 1;
            }
        }
        if (quoted || depth != 0 || out == null) {
            return NONE;
        }
        return out.toArray(NONE);
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
