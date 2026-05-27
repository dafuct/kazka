package com.kazka.story.translation;

import org.springframework.stereotype.Component;

@Component
public class TranslationPromptBuilder {

    public String buildUserMessage(String sourceLanguage, String targetLanguage, String content) {
        String sourceName = languageName(sourceLanguage);
        String targetName = languageName(targetLanguage);
        return "Translate the following " + sourceName + " fairy tale into natural, child-friendly "
                + targetName + ". Preserve paragraph breaks. Preserve dashes and decorative lines exactly. "
                + "Match the tone — warm, simple, suitable for ages 3–12. Output the translation only, "
                + "no commentary.\n\n"
                + content;
    }

    private static String languageName(String code) {
        return switch (code) {
            case "uk" -> "Ukrainian";
            case "en" -> "English";
            default -> code;
        };
    }
}
