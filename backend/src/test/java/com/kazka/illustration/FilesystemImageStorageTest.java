package com.kazka.illustration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilesystemImageStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storePanel_returnsBareKeyAndWritesFile() throws IOException {
        FilesystemImageStorage storage = new FilesystemImageStorage(tempDir.toString());
        byte[] bytes = "fake-png-p1".getBytes();

        String key = storage.storePanel("story-123", 1, bytes);

        assertThat(key).isEqualTo("story-123-p1.png");
        Path file = tempDir.resolve("story-123-p1.png");
        assertThat(file).exists();
        assertThat(Files.readAllBytes(file)).isEqualTo(bytes);
    }

    @Test
    void storePanel_secondPanel_returnsBareKeyAndWritesFile() throws IOException {
        FilesystemImageStorage storage = new FilesystemImageStorage(tempDir.toString());
        byte[] bytes = "fake-png-p2".getBytes();

        String key = storage.storePanel("story-456", 2, bytes);

        assertThat(key).isEqualTo("story-456-p2.png");
        assertThat(Files.readAllBytes(tempDir.resolve("story-456-p2.png"))).isEqualTo(bytes);
    }

    @Test
    void urlFor_prependsUploadsPath() {
        FilesystemImageStorage storage = new FilesystemImageStorage(tempDir.toString());

        assertThat(storage.urlFor("story-123-p1.png")).isEqualTo("/uploads/story-123-p1.png");
    }

    @Test
    void urlFor_null_returnsNull() {
        FilesystemImageStorage storage = new FilesystemImageStorage(tempDir.toString());

        assertThat(storage.urlFor(null)).isNull();
    }

    @Test
    void storePanel_throwsUncheckedIOException_whenWriteFails() throws IOException {
        FilesystemImageStorage storage = new FilesystemImageStorage(tempDir.toString());
        Files.createDirectory(tempDir.resolve("blocker-p1.png"));

        assertThatThrownBy(() -> storage.storePanel("blocker", 1, new byte[]{1, 2}))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("blocker");
    }

    @Test
    void deleteByKey_removesFile() throws IOException {
        FilesystemImageStorage storage = new FilesystemImageStorage(tempDir.toString());
        Files.writeString(tempDir.resolve("story-xyz-p1.png"), "panel-1");

        storage.deleteByKey("story-xyz-p1.png");

        assertThat(tempDir.resolve("story-xyz-p1.png")).doesNotExist();
    }

    @Test
    void deleteByKey_doesNothingIfFileAbsent() {
        FilesystemImageStorage storage = new FilesystemImageStorage(tempDir.toString());
        storage.deleteByKey("nonexistent-p1.png");
    }

    @Test
    void deleteByKey_doesNothingForNullOrBlank() {
        FilesystemImageStorage storage = new FilesystemImageStorage(tempDir.toString());
        storage.deleteByKey(null);
        storage.deleteByKey("");
        storage.deleteByKey("   ");
    }
}
