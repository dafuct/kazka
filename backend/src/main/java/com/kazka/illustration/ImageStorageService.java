package com.kazka.illustration;

import com.kazka.config.UploadsProperties;
import com.kazka.story.Theme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class ImageStorageService {

    private final Path uploadsDir;

    @Autowired
    public ImageStorageService(UploadsProperties props) {
        this(props.getDir());
    }

    ImageStorageService(String dir) {
        this.uploadsDir = Path.of(dir);
        try {
            Files.createDirectories(uploadsDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create uploads directory", e);
        }
    }

    public String savePng(String storyId, Theme theme, byte[] imageBytes) {
        String filename = storyId + "-" + theme.slug() + ".png";
        Path file = uploadsDir.resolve(filename);
        try {
            Files.write(file, imageBytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot save PNG for story " + storyId + " theme " + theme, e);
        }
        return "/uploads/" + filename;
    }

    public void delete(String storyId) {
        tryDelete(uploadsDir.resolve(storyId + ".png"));
        tryDelete(uploadsDir.resolve(storyId + ".svg"));
        tryDelete(uploadsDir.resolve(storyId + "-light.png"));
        tryDelete(uploadsDir.resolve(storyId + "-dark.png"));
    }

    private void tryDelete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", file, e.getMessage());
        }
    }
}
