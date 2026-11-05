package com.example.charboost.model;

import java.util.HashSet;
import java.util.Iterator;

/**
 * An entity's tag set that tells the census about every change, however it is made (addTag, removeTag,
 * a script changing getTags() directly, loading from disk). It only reports while the entity is in the
 * world: {@link #attach} counts what's there, {@link #detach} takes it all back off.
 */
public final class TrackedTags extends HashSet<String> {
    private transient TagCensus census;

    public void attach(TagCensus to) {
        if (census != null) {
            return;
        }
        census = to;
        for (String tag : this) {
            to.add(tag);
        }
    }

    public void detach() {
        TagCensus c = census;
        if (c == null) {
            return;
        }
        census = null;
        for (String tag : this) {
            c.remove(tag);
        }
    }

    public boolean attached() {
        return census != null;
    }

    @Override
    public boolean add(String tag) {
        boolean changed = super.add(tag);
        if (changed && census != null) {
            census.add(tag);
        }
        return changed;
    }

    @Override
    public boolean remove(Object tag) {
        boolean changed = super.remove(tag);
        if (changed && census != null) {
            census.remove((String) tag);
        }
        return changed;
    }

    @Override
    public void clear() {
        if (census != null) {
            for (String tag : this) {
                census.remove(tag);
            }
        }
        super.clear();
    }

    /** removeAll, retainAll and removeIf all work through the iterator, so this catches those too. */
    @Override
    public Iterator<String> iterator() {
        Iterator<String> real = super.iterator();
        return new Iterator<>() {
            private String last;

            @Override
            public boolean hasNext() {
                return real.hasNext();
            }

            @Override
            public String next() {
                last = real.next();
                return last;
            }

            @Override
            public void remove() {
                real.remove();
                if (census != null) {
                    census.remove(last);
                }
            }
        };
    }

    @Override
    public Object clone() {
        // A copy has no owner and must not report to the census.
        TrackedTags copy = new TrackedTags();
        for (String tag : this) {
            copy.add(tag);
        }
        return copy;
    }
}
