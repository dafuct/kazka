package com.kazka.narration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FilesystemAudioStorageTest {

    @Test
    void should_writeUnderExtensionAndResolveUploadsUrl_when_storingMp3(@TempDir Path dir) throws Exception {
        FilesystemAudioStorage storage = new FilesystemAudioStorage(dir.toString());
        byte[] mp3 = {'I', 'D', '3', 0x04};

        String key = storage.storeNarration("story-1", mp3, "audio/mpeg", "mp3");

        assertThat(key).isEqualTo("story-1.mp3");
        assertThat(Files.readAllBytes(dir.resolve("story-1.mp3"))).isEqualTo(mp3);
        assertThat(storage.urlFor(key)).isEqualTo("/uploads/story-1.mp3");
        assertThat(storage.urlFor(null)).isNull();
    }

    @Test
    void should_deleteFile_when_deleteByKey(@TempDir Path dir) throws Exception {
        FilesystemAudioStorage storage = new FilesystemAudioStorage(dir.toString());
        storage.storeNarration("story-2", new byte[]{1, 2}, "audio/mpeg", "mp3");
        assertThat(Files.exists(dir.resolve("story-2.mp3"))).isTrue();

        storage.deleteByKey("story-2.mp3");

        assertThat(Files.exists(dir.resolve("story-2.mp3"))).isFalse();
    }
}
