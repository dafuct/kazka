package com.kazka.illustration;

import com.kazka.story.Theme;
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
    void store_light_returnsBareKeyAndWritesFile() throws IOException {
        FilesystemImageStorage storage = new FilesystemImageStorage(tempDir.toString());
        byte[] bytes = "fake-png-light".getBytes();

        String key = storage.store("story-123", Theme.LIGHT, bytes);

        assertThat(key).isEqualTo("story-123-light.png");
        Path file = tempDir.resolve("story-123-light.png");
        assertThat(file).exists();
        assertThat(Files.readAllBytes(file)).isEqualTo(bytes);
    }

    @Test
    void store_dark_returnsBareKeyAndWritesFile() throws IOException {
        FilesystemImageStorage storage = new FilesystemImageStorage(tempDir.toString());
        byte[] bytes = "fake-png-dark".getBytes();

        String key = storage.store("story-456", Theme.DARK, bytes);

        assertThat(key).isEqualTo("story-456-dark.png");
        assertThat(Files.readAllBytes(tempDir.resolve("story-456-dark.png"))).isEqualTo(bytes);
    }

    @Test
    void urlFor_prependsUploadsPath() {
        FilesystemImageStorage storage = new FilesystemImageStorage(tempDir.toString());

        assertThat(storage.urlFor("story-123-light.png")).isEqualTo("/uploads/story-123-light.png");
    }

    @Test
    void urlFor_null_returnsNull() {
        FilesystemImageStorage storage = new FilesystemImageStorage(tempDir.toString());

        assertThat(storage.urlFor(null)).isNull();
    }

    @Test
    void store_throwsUncheckedIOException_whenWriteFails() throws IOException {
        FilesystemImageStorage storage = new FilesystemImageStorage(tempDir.toString());
        Files.createDirectory(tempDir.resolve("blocker-light.png"));

        assertThatThrownBy(() -> storage.store("blocker", Theme.LIGHT, new byte[]{1, 2}))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("blocker");
    }

    @Test
    void delete_removesAllVariants() throws IOException {
        FilesystemImageStorage storage = new FilesystemImageStorage(tempDir.toString());
        Files.writeString(tempDir.resolve("story-xyz.png"), "legacy");
        Files.writeString(tempDir.resolve("story-xyz.svg"), "<svg/>");
        Files.writeString(tempDir.resolve("story-xyz-light.png"), "light");
        Files.writeString(tempDir.resolve("story-xyz-dark.png"), "dark");

        storage.delete("story-xyz");

        assertThat(tempDir.resolve("story-xyz.png")).doesNotExist();
        assertThat(tempDir.resolve("story-xyz.svg")).doesNotExist();
        assertThat(tempDir.resolve("story-xyz-light.png")).doesNotExist();
        assertThat(tempDir.resolve("story-xyz-dark.png")).doesNotExist();
    }

    @Test
    void delete_doesNothingIfFilesAbsent() {
        FilesystemImageStorage storage = new FilesystemImageStorage(tempDir.toString());
        storage.delete("nonexistent");
    }
}
