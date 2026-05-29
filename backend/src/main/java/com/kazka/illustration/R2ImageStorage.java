package com.kazka.illustration;

import com.kazka.story.Theme;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.List;

/**
 * Stores illustrations in a private Cloudflare R2 (S3-compatible) bucket and serves them as
 * short-lived presigned GET URLs. The bucket is never public: the backend only mints a signed
 * link for a story the requesting user is already authorized to see.
 */
@Slf4j
public class R2ImageStorage implements ImageStorage {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final Duration presignTtl;

    public R2ImageStorage(S3Client s3, S3Presigner presigner, String bucket, Duration presignTtl) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
        this.presignTtl = presignTtl;
    }

    @Override
    public String store(String storyId, Theme theme, byte[] png) {
        String key = storyId + "-" + theme.slug() + ".png";
        s3.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType("image/png").build(),
                RequestBody.fromBytes(png));
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
    public void delete(String storyId) {
        List<String> keys = List.of(
                storyId + ".png", storyId + ".svg",
                storyId + "-light.png", storyId + "-dark.png");
        for (String key : keys) {
            try {
                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            } catch (Exception e) {
                log.warn("Could not delete R2 object {}: {}", key, e.getMessage());
            }
        }
    }
}
