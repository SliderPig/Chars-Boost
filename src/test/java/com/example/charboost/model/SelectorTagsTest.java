package com.example.charboost.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SelectorTagsTest {
    private static void check(String selector, String... expected) {
        assertArrayEquals(expected, SelectorTags.required(selector), selector);
    }

    @Test
    void readsPlainTags() {
        check("@e[tag=foo]", "foo");
        check("@e[type=palladium:custom_projectile,tag=FSang.Ice.Beam]", "FSang.Ice.Beam");
        check("@e[tag=a,tag=b]", "a", "b");
        check("@e[tag=a,type=zombie,tag=b,limit=1]", "a", "b");
        check("@e[ tag = spaced ]", "spaced");
    }

    @Test
    void repeatsCountOnce() {
        check("@e[tag=a,tag=a]", "a");
    }

    @Test
    void notAndEmptyDontRequireAnybody() {
        check("@e[tag=!foo]");
        check("@e[tag=]");
        check("@e[tag=!foo,tag=bar]", "bar");
    }

    @Test
    void ignoresTagInsideOtherOptions() {
        check("@e[nbt={Tags:[\"foo\"]}]");
        check("@e[nbt={tag=foo,bar:1},tag=real]", "real");
        check("@e[scores={tag=1..}]");
        check("@e[name=\"tag=foo\"]");
        check("@e[name=\"a,tag=foo\",tag=real]", "real");
    }

    @Test
    void anythingOddGivesNothing() {
        check("@e");
        check("Steve");
        check("");
        check("@e[tag=foo");
        check("@e[nbt={a:1]");
        check("@e[tag=\"quoted\"]");
    }

    @Test
    void nullGivesNothing() {
        assertEquals(0, SelectorTags.required(null).length);
    }

    @Test
    void otherSelectorsToo() {
        check("@a[tag=x]", "x");
        check("@s[tag=x]", "x");
        check("@p[tag=x]", "x");
        check("@r[tag=x]", "x");
    }

    @Test
    void readsTheItemADroppedItemSelectorWants() {
        assertEquals("fsang:stormbreaker", SelectorTags.requiredItem("@e[type=item,nbt={Item:{id:\"fsang:stormbreaker\"}}]"));
        assertEquals("minecraft:stone", SelectorTags.requiredItem("@e[type=minecraft:item,nbt={Item:{id:\"minecraft:stone\",Count:1b}}]"));
        assertEquals("a:b", SelectorTags.requiredItem("@e[nbt={Item:{id:\"a:b\"}},type=item,limit=1]"));
    }

    @Test
    void leavesOtherNbtSelectorsAlone() {
        // Not narrowed to dropped items
        assertEquals(null, SelectorTags.requiredItem("@e[nbt={Item:{id:\"a:b\"}}]"));
        assertEquals(null, SelectorTags.requiredItem("@e[type=zombie,nbt={Item:{id:\"a:b\"}}]"));
        // "must not be"
        assertEquals(null, SelectorTags.requiredItem("@e[type=item,nbt=!{Item:{id:\"a:b\"}}]"));
        // Something else first, or nothing about the id
        assertEquals(null, SelectorTags.requiredItem("@e[type=item,nbt={Item:{Count:1b,id:\"a:b\"}}]"));
        assertEquals(null, SelectorTags.requiredItem("@e[type=item,nbt={Age:5s}]"));
        assertEquals(null, SelectorTags.requiredItem("@e[type=item]"));
        assertEquals(null, SelectorTags.requiredItem("@e"));
    }

    @Test
    void customSelectorLettersAreLeftAlone() {
        check("@q[tag=x]");
    }
}
