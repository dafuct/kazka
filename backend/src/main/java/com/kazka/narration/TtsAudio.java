package com.kazka.narration;

/**
 * Final, browser-playable narration audio produced by a {@link TtsClient}: the encoded bytes
 * plus how to store and serve them ({@code contentType} for the object store, {@code
 * fileExtension} for the storage key). Providers pick their own container — ElevenLabs returns
 * MP3, the Gemini path returns WAV.
 */
public record TtsAudio(byte[] bytes, String contentType, String fileExtension) {}
