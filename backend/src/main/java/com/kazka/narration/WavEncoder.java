package com.kazka.narration;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Wraps raw little-endian PCM in a canonical 44-byte RIFF/WAVE header so a browser
 * {@code <audio>} element can play it. Gemini TTS returns headerless PCM
 * (mime {@code audio/L16;rate=24000} — 24 kHz, signed 16-bit, mono).
 */
@Component
public class WavEncoder {

    private static final int SAMPLE_RATE = 24_000;
    private static final int CHANNELS = 1;
    private static final int BITS_PER_SAMPLE = 16;

    /** Wrap 24 kHz / mono / 16-bit PCM as a WAV byte[]. */
    public byte[] wrap24kMono16(byte[] pcm) {
        int byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8;
        int blockAlign = CHANNELS * BITS_PER_SAMPLE / 8;
        int dataSize = pcm.length;

        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + dataSize);
        writeAscii(out, "RIFF");
        writeIntLe(out, 36 + dataSize);
        writeAscii(out, "WAVE");
        writeAscii(out, "fmt ");
        writeIntLe(out, 16);               // PCM fmt subchunk size
        writeShortLe(out, 1);              // audio format = PCM
        writeShortLe(out, CHANNELS);
        writeIntLe(out, SAMPLE_RATE);
        writeIntLe(out, byteRate);
        writeShortLe(out, blockAlign);
        writeShortLe(out, BITS_PER_SAMPLE);
        writeAscii(out, "data");
        writeIntLe(out, dataSize);
        out.writeBytes(pcm);
        return out.toByteArray();
    }

    private static void writeAscii(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
    }

    private static void writeIntLe(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
        out.write((v >> 16) & 0xFF);
        out.write((v >> 24) & 0xFF);
    }

    private static void writeShortLe(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
    }
}
