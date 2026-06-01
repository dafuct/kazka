package com.kazka.comics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One beat of a comic-style tale: a visual scene description (art only, no
 * lettering) and any short dialog lines to draw as speech bubbles.
 *
 * Produced by {@link ActsStructurer} as a list of 5 (see ActsStructurer.BEATS).
 * The single-page render has no per-beat aspect ratio.
 */
public record Act(
        @JsonProperty("scene") String scene,
        @JsonProperty("narration") String narration,
        @JsonProperty("dialog") List<Dialog> dialog
) {
    public record Dialog(
            @JsonProperty("speaker") String speaker,
            @JsonProperty("line") String line
    ) {}

    @JsonCreator
    public Act { // compact constructor — normalize nulls (the LLM may omit fields)
        scene = scene == null ? "" : scene;
        narration = narration == null ? "" : narration;
        dialog = dialog == null ? List.of() : List.copyOf(dialog);
    }
}
