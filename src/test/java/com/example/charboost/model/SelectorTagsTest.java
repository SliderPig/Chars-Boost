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
    void customSelectorLettersAreLeftAlone() {
        check("@q[tag=x]");
    }
}
