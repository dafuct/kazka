package com.kazka.narration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FilesystemAudioStorageTest {

    @Test
    void should_writeWavAndResolveUploadsUrl_when_storingNarration(@TempDir Path dir) throws Exception {
        FilesystemAudioStorage storage = new FilesystemAudioStorage(dir.toString());
        byte[] wav = {0x52, 0x49, 0x46, 0x46}; // "RIFF"

        String key = storage.storeNarration("story-1", wav);

        assertThat(key).isEqualTo("story-1.wav");
        assertThat(Files.readAllBytes(dir.resolve("story-1.wav"))).isEqualTo(wav);
        assertThat(storage.urlFor(key)).isEqualTo("/uploads/story-1.wav");
        assertThat(storage.urlFor(null)).isNull();
    }

    @Test
    void should_deleteFile_when_deleteByKey(@TempDir Path dir) throws Exception {
        FilesystemAudioStorage storage = new FilesystemAudioStorage(dir.toString());
        storage.storeNarration("story-2", new byte[]{1, 2});
        assertThat(Files.exists(dir.resolve("story-2.wav"))).isTrue();

        storage.deleteByKey("story-2.wav");

        assertThat(Files.exists(dir.resolve("story-2.wav"))).isFalse();
    }
}
