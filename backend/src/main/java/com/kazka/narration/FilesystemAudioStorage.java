package com.kazka.narration;

import com.kazka.config.UploadsProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stores narration WAVs on the local filesystem and serves them via {@code /uploads/}. */
@Slf4j
public class FilesystemAudioStorage implements AudioStorage {

    private final Path uploadsDir;

    public FilesystemAudioStorage(UploadsProperties props) {
        this(props.getDir());
    }

    FilesystemAudioStorage(String dir) {
        this.uploadsDir = Path.of(dir);
        try {
            Files.createDirectories(uploadsDir);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Cannot create uploads directory", ioException);
        }
    }

    @Override
    public String storeNarration(String storyId, byte[] wav) {
        String key = storyId + ".wav";
        try {
            Files.write(uploadsDir.resolve(key), wav);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Cannot save narration WAV for story " + storyId, ioException);
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
        } catch (IOException ioException) {
            log.warn("Could not delete narration file {}: {}", key, ioException.getMessage());
        }
    }
}
