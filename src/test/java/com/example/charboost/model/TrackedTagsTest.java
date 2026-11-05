package com.example.charboost.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrackedTagsTest {
    @Test
    void attachCountsWhatIsThereAndDetachTakesItBack() {
        TagCensus census = new TagCensus();
        TrackedTags tags = new TrackedTags();
        tags.add("a");
        assertFalse(census.anyHave("a"), "not in the world yet");
        tags.attach(census);
        assertTrue(census.anyHave("a"));
        tags.detach();
        assertFalse(census.anyHave("a"));
    }

    @Test
    void attachingTwiceCountsOnce() {
        TagCensus census = new TagCensus();
        TrackedTags tags = new TrackedTags();
        tags.add("a");
        tags.attach(census);
        tags.attach(census);
        assertEquals(1, census.count("a"));
    }

    @Test
    void changesWhileAttachedAreCounted() {
        TagCensus census = new TagCensus();
        TrackedTags tags = new TrackedTags();
        tags.attach(census);
        tags.add("a");
        tags.add("a");
        assertEquals(1, census.count("a"));
        tags.remove("a");
        assertFalse(census.anyHave("a"));
        tags.remove("a");
        assertEquals(0, census.count("a"));
    }

    @Test
    void everyWayOfChangingTheSetIsCaught() {
        TagCensus census = new TagCensus();
        TrackedTags tags = new TrackedTags();
        tags.attach(census);

        tags.addAll(List.of("a", "b", "c", "d"));
        assertEquals(1, census.count("d"));

        tags.removeAll(List.of("a"));
        assertFalse(census.anyHave("a"));

        tags.retainAll(List.of("b", "c"));
        assertFalse(census.anyHave("d"));
        assertTrue(census.anyHave("b"));

        tags.removeIf("b"::equals);
        assertFalse(census.anyHave("b"));

        Iterator<String> it = tags.iterator();
        it.next();
        it.remove();
        assertFalse(census.anyHave("c"));
        assertEquals(0, census.size());

        tags.add("x");
        tags.clear();
        assertFalse(census.anyHave("x"));
        assertTrue(tags.isEmpty());
    }

    @Test
    void twoEntitiesWithTheSameTag() {
        TagCensus census = new TagCensus();
        TrackedTags one = new TrackedTags();
        TrackedTags two = new TrackedTags();
        one.add("a");
        two.add("a");
        one.attach(census);
        two.attach(census);
        assertEquals(2, census.count("a"));
        one.detach();
        assertTrue(census.anyHave("a"));
        two.detach();
        assertFalse(census.anyHave("a"));
    }

    @Test
    void anyHaveAllNeedsEveryTag() {
        TagCensus census = new TagCensus();
        census.add("a");
        assertTrue(census.anyHaveAll(new String[]{"a"}));
        assertFalse(census.anyHaveAll(new String[]{"a", "b"}));
        assertTrue(census.anyHaveAll(new String[0]));
    }

    @Test
    void aCopyDoesNotReport() {
        TagCensus census = new TagCensus();
        TrackedTags tags = new TrackedTags();
        tags.add("a");
        tags.attach(census);
        @SuppressWarnings("unchecked")
        java.util.HashSet<String> copy = (java.util.HashSet<String>) tags.clone();
        copy.add("b");
        assertFalse(census.anyHave("b"));
        assertEquals(1, census.count("a"));
    }

    @Test
    void behavesLikeASet() {
        TrackedTags tags = new TrackedTags();
        assertTrue(tags.add("a"));
        assertFalse(tags.add("a"));
        assertTrue(tags.contains("a"));
        assertEquals(1, tags.size());
        assertTrue(tags.remove("a"));
        assertFalse(tags.remove("a"));
    }
}
