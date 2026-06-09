package com.kazka.narration;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

/**
 * Stores narration WAVs in the private Cloudflare R2 bucket and serves them as short-lived
 * presigned GET URLs. Keys live under a {@code narration/} prefix ({@code narration/<storyId>.wav}).
 * Mirrors {@link com.kazka.illustration.R2ImageStorage}.
 */
@Slf4j
public class R2AudioStorage implements AudioStorage {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final Duration presignTtl;

    public R2AudioStorage(S3Client s3, S3Presigner presigner, String bucket, Duration presignTtl) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
        this.presignTtl = presignTtl;
    }

    @Override
    public String storeNarration(String storyId, byte[] wav) {
        String key = "narration/" + storyId + ".wav";
        s3.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType("audio/wav").build(),
                RequestBody.fromBytes(wav));
        return key;
    }

    @Override
    public String urlFor(String key) {
        if (key == null) {
            return null;
        }
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(presignTtl)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build();
        return presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public void deleteByKey(String key) {
        if (key == null || key.isBlank()) return;
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception exception) {
            log.warn("Could not delete R2 narration object {}: {}", key, exception.getMessage());
        }
    }
}
