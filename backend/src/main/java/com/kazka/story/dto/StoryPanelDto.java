package com.kazka.story.dto;

import com.kazka.comics.PanelAspect;
import com.kazka.comics.StoryPanel;
import com.kazka.illustration.ImageUrlResolver;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record StoryPanelDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int panelIndex,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String imageUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String narration,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<DialogLine> dialog,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PanelAspect aspect
) {
    public record DialogLine(String speaker, String line) {}

    public static StoryPanelDto from(StoryPanel panel, ImageUrlResolver resolver) {
        List<DialogLine> lines = panel.getDialog().stream()
                .map(d -> new DialogLine(d.speaker(), d.line()))
                .toList();
        return new StoryPanelDto(
                panel.getPanelIndex(),
                resolver.urlFor(panel.getImagePath()),
                panel.getNarration(),
                lines,
                panel.getAspect()
        );
    }
}
