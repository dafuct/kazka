package com.kazka.story;

public enum Theme {
    LIGHT, DARK;

    public String slug() {
        return name().toLowerCase();
    }
}
