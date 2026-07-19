package com.kazka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Config slot for LLM + image providers. Text/editor/scene/judge use Google Gemini 2.5 Flash
 * via the OpenAI-compatible endpoint; comics panels use Google's native Nano Banana endpoint
 * (gemini-2.5-flash-image). Both share the single {@code GOOGLE_API_KEY}. Migrated off the
 * HuggingFace Inference Router on 2026-05-30 (see
 * wiki/lessons/hf-router-strict-mode-rejects-repetition-penalty.md) and off Fal.ai on
 * 2026-05-31 when comics replaced single-cover illustrations.
 */
@ConfigurationProperties("kazka.ai")
@Getter
@Setter
public class AiProviderProperties {

    private String apiToken = null;        // Gemini key (GOOGLE_API_KEY env slot)
    private String textModel = "gemini-2.5-flash";
    private String editorModel = "gemini-2.5-flash";
    private String sceneModel = "gemini-2.5-flash";
    private String textBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai";

    // Nano Banana (Gemini 2.5 Flash Image) — Google's native multimodal image endpoint.
    // Auth via x-goog-api-key header (NOT Authorization: Bearer); reuses GOOGLE_API_KEY.
    private String nanoBananaModel = "gemini-2.5-flash-image";
    private String nanoBananaBaseUrl = "https://generativelanguage.googleapis.com/v1beta";

    // Gemini 2.5 Flash TTS — native v1beta generateContent with response_modalities=["AUDIO"].
    // Reuses the Nano Banana base URL (same generativelanguage v1beta root) and GOOGLE_API_KEY.
    // NOTE: the model id is a preview name and may change — verify against the live API.
    private String ttsModel = "gemini-2.5-flash-preview-tts";
    private String ttsVoice = "Sulafat"; // warm female; swappable
    private String ttsStylePrompt = "Прочитай цю казку повільно й тепло, як бабуся розповідає на ніч:";

    // --- TTS provider selection + ElevenLabs (lifelike neural narration voice, bilingual) ---
    // Read-aloud synthesizes via this provider. ElevenLabs is the default; the Gemini fields
    // above are retained for the fallback provider (kazka.ai.tts-provider=gemini).
    private String ttsProvider = "elevenlabs";             // elevenlabs | gemini
    private final ElevenLabs elevenlabs = new ElevenLabs();

    private double textTemperature = 0.75;
    private double textTopP = 0.9;
    // Gemini's OpenAI-compat endpoint rejects frequency_penalty/presence_penalty with 400.
    // Defaults are 0.0 so the client skips sending them. Override via env only if the runtime
    // endpoint is OpenAI/OpenRouter/Together — those DO accept the OpenAI-standard penalties.
    private double textFrequencyPenalty = 0.0;
    private double textPresencePenalty = 0.0;
    private int textMaxTokens = 4096;

    private double editorTemperature = 0.3;
    private double editorTopP = 0.9;
    private double editorFrequencyPenalty = 0.0;
    private double editorPresencePenalty = 0.0;
    private int editorMaxTokens = 4096;

    // --- Comics pipeline config (panels per tale, aspects, timeout) ---
    private final Comics comics = new Comics();

    @Getter
    @Setter
    public static class Comics {
        private int panelsPerTale = 4;
        private List<PanelAspectName> panelAspects =
            List.of(PanelAspectName.LANDSCAPE, PanelAspectName.SQUARE,
                              PanelAspectName.SQUARE, PanelAspectName.LANDSCAPE);
        private Duration pipelineTimeout = Duration.ofSeconds(60);
        private int maxConcurrentPerUser = 1;
    }

    /** Lower-cased mirror of com.kazka.comics.PanelAspect for property binding. */
    public enum PanelAspectName { LANDSCAPE, SQUARE }

    /** ElevenLabs TTS config. Voices are keyed by tale language (e.g. uk/en). */
    @Getter
    @Setter
    public static class ElevenLabs {
        private String apiKey = null;                          // ELEVENLABS_API_KEY
        private String baseUrl = "https://api.elevenlabs.io";
        private String model = "eleven_multilingual_v2";
        private String outputFormat = "mp3_44100_128";
        private Map<String, String> voices = new HashMap<>();  // language -> voiceId
        private final VoiceSettings voiceSettings = new VoiceSettings();

        /** Voice id for a tale language, falling back to the uk voice when unset. */
        public String voiceFor(String language) {
            String v = language == null ? null : voices.get(language);
            if (v == null || v.isBlank()) {
                v = voices.get("uk");
            }
            return v;
        }
    }

    /** ElevenLabs voice_settings — tuned for warm bedtime storytelling; adjust by ear via config. */
    @Getter
    @Setter
    public static class VoiceSettings {
        private double stability = 0.45;
        private double similarityBoost = 0.75;
        private double style = 0.30;
        private double speed = 0.92;
        private boolean useSpeakerBoost = true;
    }
}
