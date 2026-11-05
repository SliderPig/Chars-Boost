package com.example.charboost.hook;

/** Added to EntitySelector: the tags it insists on, read from its text when it was parsed. */
public interface SelectorState {
    void charboost$tags(String[] tags);
}
