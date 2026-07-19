package com.kazka.story.branching;

import com.kazka.child.Character;
import com.kazka.child.ChildProfile;
import com.kazka.story.dto.GenerationRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.List;

@Component
public class BranchingPromptBuilder {

    private final String branchingSystem;

    public BranchingPromptBuilder() {
        try {
            this.branchingSystem = new ClassPathResource("prompts/branching-system.md")
                    .getContentAsString(StandardCharsets.UTF_8).strip();
        } catch (IOException ioException) {
            throw new UncheckedIOException("Cannot read prompt file: prompts/branching-system.md", ioException);
        }
    }

    /**
     * System prompt for branching segments. Unlike the one-shot {@code buildStorySystem}, this
     * tells the model to write ONLY the requested segment and never restate/restart the tale —
     * reusing the complete-tale prompt here made the model re-emit a full titled story on every
     * choice, tripling the content.
     */
    public String buildBranchingSystem() {
        return branchingSystem;
    }

    private static String languageName(String code) {
        return "en".equalsIgnoreCase(code) ? "English" : "Ukrainian";
    }

    public String buildOpeningUserMessage(GenerationRequest req, ChildProfile child, List<Character> recurringCast) {
        StringBuilder sb = new StringBuilder();
        sb.append("Write the OPENING of a branching fairy tale (100-150 words), in ")
          .append(languageName(req.language())).append(".\n\n")
          .append("Theme: ").append(req.theme()).append('\n')
          .append("Characters: ").append(String.join(", ", req.characters())).append('\n')
          .append("Age group: ").append(req.ageGroup()).append('\n');

        if (child != null && child.getName() != null && !child.getName().isBlank()) {
            sb.append("Hero's name: ").append(child.getName()).append('\n');
            if (child.getBirthYear() != null) {
                int age = Year.now().getValue() - child.getBirthYear();
                sb.append("Hero's approximate age: ").append(age).append('\n');
            }
        }
        if (recurringCast != null && !recurringCast.isEmpty()) {
            sb.append('\n').append("RECURRING CAST (these characters return from previous tales — keep them consistent):\n");
            for (Character c : recurringCast) {
                sb.append("- ").append(c.getName()).append(" (").append(c.getKind()).append("): ").append(c.getDescription());
                if (c.getTraits() != null && !c.getTraits().isEmpty()) {
                    sb.append(" — traits: ").append(String.join(", ", c.getTraits()));
                }
                sb.append('\n');
            }
        }
        sb.append('\n').append("End the opening at a natural decision point. ")
          .append("Return the OPENING JSON object with keys \"title\", \"segment\", \"choiceA\", \"choiceB\".");
        return sb.toString();
    }

    public String buildMiddleUserMessage(String accumulatedContent, String chosenOption, String language) {
        return "You are continuing an interactive fairy tale, written in " + languageName(language) + ".\n\n"
                + "CONTEXT — the story so far (do NOT repeat, quote, or restate any of it):\n\n"
                + accumulatedContent + "\n\n"
                + "The reader chose: " + chosenOption + "\n\n"
                + "Write ONLY the MIDDLE (100-150 words) that comes next — continue seamlessly from the "
                + "last sentence above, do not restart, do not re-introduce the hero, end at another decision point. "
                + "Return the MIDDLE JSON object with keys \"segment\", \"choiceA\", \"choiceB\".";
    }

    public String buildClosingUserMessage(String accumulatedContent, String chosenOption, String language) {
        return "You are finishing an interactive fairy tale, written in " + languageName(language) + ".\n\n"
                + "CONTEXT — the story so far (do NOT repeat, quote, or restate any of it):\n\n"
                + accumulatedContent + "\n\n"
                + "The reader chose: " + chosenOption + "\n\n"
                + "Write ONLY the CLOSING (100-150 words) that comes next — continue seamlessly from the "
                + "last sentence above, do not restart, and resolve the tale warmly. "
                + "Return the CLOSING JSON object with the single key \"segment\".";
    }
}
