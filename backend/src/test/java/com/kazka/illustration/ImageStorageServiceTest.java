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

class ImageStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void savePng_light_writesFileWithLightSuffix() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        byte[] bytes = "fake-png-light".getBytes();

        String path = service.savePng("story-123", Theme.LIGHT, bytes);

        assertThat(path).isEqualTo("/uploads/story-123-light.png");
        Path file = tempDir.resolve("story-123-light.png");
        assertThat(file).exists();
        assertThat(Files.readAllBytes(file)).isEqualTo(bytes);
    }

    @Test
    void savePng_dark_writesFileWithDarkSuffix() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        byte[] bytes = "fake-png-dark".getBytes();

        String path = service.savePng("story-456", Theme.DARK, bytes);

        assertThat(path).isEqualTo("/uploads/story-456-dark.png");
        assertThat(Files.readAllBytes(tempDir.resolve("story-456-dark.png"))).isEqualTo(bytes);
    }

    @Test
    void savePng_throwsUncheckedIOException_whenWriteFails() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        Files.createDirectory(tempDir.resolve("blocker-light.png"));

        assertThatThrownBy(() -> service.savePng("blocker", Theme.LIGHT, new byte[]{1, 2}))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("blocker");
    }

    @Test
    void delete_removesAllVariants() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        Files.writeString(tempDir.resolve("story-xyz.png"), "legacy");
        Files.writeString(tempDir.resolve("story-xyz.svg"), "<svg/>");
        Files.writeString(tempDir.resolve("story-xyz-light.png"), "light");
        Files.writeString(tempDir.resolve("story-xyz-dark.png"), "dark");

        service.delete("story-xyz");

        assertThat(tempDir.resolve("story-xyz.png")).doesNotExist();
        assertThat(tempDir.resolve("story-xyz.svg")).doesNotExist();
        assertThat(tempDir.resolve("story-xyz-light.png")).doesNotExist();
        assertThat(tempDir.resolve("story-xyz-dark.png")).doesNotExist();
    }

    @Test
    void delete_doesNothingIfFilesAbsent() {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        service.delete("nonexistent");
    }
}
