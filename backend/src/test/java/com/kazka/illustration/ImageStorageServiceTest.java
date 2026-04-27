package com.kazka.illustration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ImageStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void save_writesFileToDisk() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        byte[] imageBytes = "fake-png-data".getBytes();
        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        String path = service.save("story-123", base64);

        assertThat(path).isEqualTo("/uploads/story-123.png");
        Path file = tempDir.resolve("story-123.png");
        assertThat(file).exists();
        assertThat(Files.readAllBytes(file)).isEqualTo(imageBytes);
    }

    @Test
    void delete_removesFile() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        Path file = tempDir.resolve("story-456.png");
        Files.writeString(file, "data");

        service.delete("story-456");

        assertThat(file).doesNotExist();
    }

    @Test
    void delete_doesNothingIfFileAbsent() {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        service.delete("nonexistent");
    }
}
