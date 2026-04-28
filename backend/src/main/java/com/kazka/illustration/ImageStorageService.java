package com.kazka.illustration;

import com.kazka.config.UploadsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);

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

    public String save(String storyId, byte[] imageBytes) {
        Path file = uploadsDir.resolve(storyId + ".png");
        try {
            Files.write(file, imageBytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot save image for story " + storyId, e);
        }
        return "/uploads/" + storyId + ".png";
    }

    public void delete(String storyId) {
        Path file = uploadsDir.resolve(storyId + ".png");
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete image for story {}: {}", storyId, e.getMessage());
        }
    }
}
