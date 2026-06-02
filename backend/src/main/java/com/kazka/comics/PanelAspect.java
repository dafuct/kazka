package com.kazka.comics;

/**
 * Aspect ratio for an image sent to Nano Banana. {@code PAGE} is the
 * full single comic-page render (portrait); {@code LANDSCAPE}/{@code SQUARE}
 * are retained for backward-compat with stored rows.
 */
public enum PanelAspect {
    LANDSCAPE,
    SQUARE,
    PAGE;

    public String aspectRatio() {
        return switch (this) {
            case LANDSCAPE -> "16:9";
            case SQUARE -> "1:1";
            case PAGE -> "3:4";
        };
    }
}
