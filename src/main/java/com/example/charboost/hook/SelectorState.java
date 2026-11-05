package com.example.charboost.hook;

/** Added to EntitySelector: the tags and dropped item it insists on, read from its text when it was parsed. */
public interface SelectorState {
    void charboost$tags(String[] tags);

    void charboost$item(String item);
}
