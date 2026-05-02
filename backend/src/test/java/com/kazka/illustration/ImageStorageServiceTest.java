package com.kazka.illustration;

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
    void save_writesFileToDisk() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        byte[] imageBytes = "fake-png-data".getBytes();

        String path = service.save("story-123", imageBytes);

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

    @Test
    void saveSvg_writesFileToDisk() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        String svgText = "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>";

        String path = service.saveSvg("story-789", svgText);

        assertThat(path).isEqualTo("/uploads/story-789.svg");
        Path file = tempDir.resolve("story-789.svg");
        assertThat(file).exists();
        assertThat(Files.readString(file)).isEqualTo(svgText);
    }

    @Test
    void delete_removesSvgFile() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        Path file = tempDir.resolve("story-abc.svg");
        Files.writeString(file, "<svg/>");

        service.delete("story-abc");

        assertThat(file).doesNotExist();
    }

    @Test
    void delete_removesBothPngAndSvg() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        Path png = tempDir.resolve("story-xyz.png");
        Path svg = tempDir.resolve("story-xyz.svg");
        Files.writeString(png, "png-data");
        Files.writeString(svg, "<svg/>");

        service.delete("story-xyz");

        assertThat(png).doesNotExist();
        assertThat(svg).doesNotExist();
    }

    @Test
    void saveSvg_throwsUncheckedIOException_whenWriteFails() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        // Replace uploads dir with a file to make the write fail
        Path blocker = tempDir.resolve("story-fail.svg");
        Files.createDirectory(blocker); // directory where a file is expected = write will fail

        assertThatThrownBy(() -> service.saveSvg("story-fail", "<svg/>"))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("story-fail");
    }
}
