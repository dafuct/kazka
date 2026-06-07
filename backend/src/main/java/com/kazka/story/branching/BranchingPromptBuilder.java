package com.kazka.story.branching;

import com.kazka.child.Character;
import com.kazka.child.ChildProfile;
import com.kazka.story.dto.GenerationRequest;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.List;

@Component
public class BranchingPromptBuilder {

    public String buildOpeningUserMessage(GenerationRequest req, ChildProfile child, List<Character> recurringCast) {
        StringBuilder sb = new StringBuilder();
        sb.append("Write the OPENING of a branching fairy tale (100-150 words).\n\n")
          .append("Language: ").append(req.language()).append('\n')
          .append("Theme: ").append(req.theme()).append('\n')
          .append("Characters: ").append(String.join(", ", req.characters())).append('\n')
          .append("Age: ").append(req.ageGroup()).append('\n');

        if (child != null && child.getName() != null && !child.getName().isBlank()) {
            sb.append("Child name: ").append(child.getName()).append('\n');
            if (child.getBirthYear() != null) {
                int age = Year.now().getValue() - child.getBirthYear();
                sb.append("Approximate age: ").append(age).append('\n');
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
        sb.append('\n').append("End the segment at a natural decision point. ")
          .append("After the segment, write EXACTLY this format:\n\n")
          .append("---\n\n")
          .append("CHOICE_A: <8-15 word description>\n")
          .append("CHOICE_B: <8-15 word description>\n");
        return sb.toString();
    }

    public String buildMiddleUserMessage(String accumulatedContent, String chosenOption) {
        return "Continue the branching fairy tale. Here is what has happened so far:\n\n"
                + accumulatedContent + "\n\n"
                + "The reader chose: " + chosenOption + "\n\n"
                + "Write the MIDDLE (100-150 words). End at another decision point. "
                + "Use the same CHOICE_A/CHOICE_B format.";
    }

    public String buildClosingUserMessage(String accumulatedContent, String chosenOption) {
        return "Continue the branching fairy tale. Here is what has happened so far:\n\n"
                + accumulatedContent + "\n\n"
                + "The reader chose: " + chosenOption + "\n\n"
                + "Write the CLOSING (100-150 words). Resolve the tale. No choice block at the end.";
    }

    public String transitionLine(ChildProfile child, String chosenOption) {
        String name = child != null && child.getName() != null ? child.getName() : "the reader";
        String lang = child != null ? child.getPreferredLanguage() : "uk";
        if ("en".equals(lang)) {
            return "\n\n— " + name + " chose: " + chosenOption + " —\n\n";
        }
        return "\n\n— " + name + " обрала: " + chosenOption + " —\n\n";
    }
}
