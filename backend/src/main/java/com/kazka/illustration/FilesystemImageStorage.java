package com.kazka.illustration;

import com.kazka.config.UploadsProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stores comics panel illustrations on the local filesystem and serves them via {@code /uploads/}. */
@Slf4j
public class FilesystemImageStorage implements ImageStorage {

    private final Path uploadsDir;

    public FilesystemImageStorage(UploadsProperties props) {
        this(props.getDir());
    }

    FilesystemImageStorage(String dir) {
        this.uploadsDir = Path.of(dir);
        try {
            Files.createDirectories(uploadsDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create uploads directory", e);
        }
    }

    @Override
    public String storePanel(String storyId, int panelIndex, byte[] png) {
        String key = storyId + "-p" + panelIndex + ".png";
        try {
            Files.write(uploadsDir.resolve(key), png);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot save panel PNG for story " + storyId + " panel " + panelIndex, e);
        }
        return key;
    }

    @Override
    public String urlFor(String key) {
        return key == null ? null : "/uploads/" + key;
    }

    @Override
    public void deleteByKey(String key) {
        if (key == null || key.isBlank()) return;
        try {
            Files.deleteIfExists(uploadsDir.resolve(key));
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", key, e.getMessage());
        }
    }
}
