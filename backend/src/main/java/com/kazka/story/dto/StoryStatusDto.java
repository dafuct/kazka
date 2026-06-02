package com.kazka.story.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record StoryStatusDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Phase status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int panelsReady,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title
) {
    public enum Phase {
        /** Story row exists but content is still streaming. */
        WRITING,
        /** Content done; acts structurer running. */
        EXTRACTING_ACTS,
        /** At least one panel call fired; some panels may already be saved. */
        DRAWING,
        /** The comic page is persisted; status flipped. */
        READY,
        /** Pipeline failed at some point. */
        FAILED
    }
}
