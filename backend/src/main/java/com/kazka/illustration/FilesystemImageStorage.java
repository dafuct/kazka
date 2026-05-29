package com.kazka.illustration;

import com.kazka.config.UploadsProperties;
import com.kazka.story.Theme;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stores illustrations on the local filesystem and serves them via the {@code /uploads/} path. */
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
    public String store(String storyId, Theme theme, byte[] png) {
        String key = storyId + "-" + theme.slug() + ".png";
        try {
            Files.write(uploadsDir.resolve(key), png);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot save PNG for story " + storyId + " theme " + theme, e);
        }
        return key;
    }

    @Override
    public String urlFor(String key) {
        return key == null ? null : "/uploads/" + key;
    }

    @Override
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
