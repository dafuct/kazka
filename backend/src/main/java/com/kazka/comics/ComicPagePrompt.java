package com.kazka.comics;

import java.util.List;

/**
 * Composes ONE image prompt for a full multi-panel comic page from the
 * structured beats. Because the whole page is a single image, character
 * consistency is intrinsic, so the style + character anchor are stated once.
 *
 * The image is purely visual — no speech bubbles, no lettering. Image models
 * render non-Latin scripts (esp. Cyrillic) unreliably for anything longer than
 * a single word, and baked-in text can't be re-translated when the reader
 * switches language. Dialog is kept in the structured beats for possible
 * future use as an HTML overlay.
 */
final class ComicPagePrompt {

    private ComicPagePrompt() {}

    private static final String STYLE_ANCHOR = """
            A single full comic-book page for children in a modern, polished cartoon style: \
            clean bold black linework, flat cel-shaded coloring, a warm vibrant but slightly \
            muted palette, soft cinematic lighting, expressive friendly faces. Crisp white \
            gutters between panels.""";

    static String compose(List<Act> beats, String language) {
        StringBuilder sb = new StringBuilder();
        sb.append(STYLE_ANCHOR).append("\n\n");
        sb.append("Lay the page out as ").append(beats.size())
          .append(" panels of varied sizes — one wide establishing panel at the top, ")
          .append("smaller panels below — read left-to-right, top-to-bottom.\n\n");
        for (int index = 0; index < beats.size(); index++) {
            Act beat = beats.get(index);
            sb.append("Panel ").append(index + 1).append(": ").append(beat.scene().strip()).append("\n");
        }
        sb.append("\nThe whole page must be 100% wordless: no speech bubbles, no captions, ")
          .append("no signs, no labels, no letters or numbers anywhere — emotions and story ")
          .append("are told only through the artwork. Show character expressions and body ")
          .append("language clearly so the action reads without dialogue.");
        // Language is unused — the image is wordless. The `language` parameter stays in the
        // signature because callers still pass it (and we may need it again if bubbles return).
        return sb.toString();
    }
}
