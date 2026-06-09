package com.kazka.narration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WavEncoderTest {

    private final WavEncoder encoder = new WavEncoder();

    private static String ascii(byte[] b, int from, int len) {
        return new String(b, from, len, StandardCharsets.US_ASCII);
    }

    private static int leInt(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8)
                | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
    }

    private static int leShort(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
    }

    @Test
    void should_prepend44ByteRiffHeader_when_wrappingPcm() {
        byte[] pcm = new byte[100];
        byte[] wav = encoder.wrap24kMono16(pcm);

        assertThat(wav).hasSize(44 + 100);
        assertThat(ascii(wav, 0, 4)).isEqualTo("RIFF");
        assertThat(ascii(wav, 8, 4)).isEqualTo("WAVE");
        assertThat(ascii(wav, 12, 4)).isEqualTo("fmt ");
        assertThat(ascii(wav, 36, 4)).isEqualTo("data");
    }

    @Test
    void should_writeCorrectFmtAndSizes_when_wrappingPcm() {
        byte[] pcm = new byte[200];
        byte[] wav = encoder.wrap24kMono16(pcm);

        assertThat(leInt(wav, 4)).isEqualTo(36 + 200);   // RIFF chunk size
        assertThat(leInt(wav, 16)).isEqualTo(16);        // fmt subchunk size
        assertThat(leShort(wav, 20)).isEqualTo(1);       // PCM format
        assertThat(leShort(wav, 22)).isEqualTo(1);       // mono
        assertThat(leInt(wav, 24)).isEqualTo(24000);     // sample rate
        assertThat(leInt(wav, 28)).isEqualTo(24000 * 2); // byte rate
        assertThat(leShort(wav, 32)).isEqualTo(2);       // block align
        assertThat(leShort(wav, 34)).isEqualTo(16);      // bits per sample
        assertThat(leInt(wav, 40)).isEqualTo(200);       // data size
    }
}
