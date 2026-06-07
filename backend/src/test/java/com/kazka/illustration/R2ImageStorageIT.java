package com.kazka.illustration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class R2ImageStorageIT {

    static final String BUCKET = "kazka-test";

    static MinIOContainer minio;
    static S3Client s3;
    static S3Presigner presigner;
    static R2ImageStorage storage;

    @BeforeAll
    static void start() {
        minio = new MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z");
        minio.start();

        var creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(minio.getUserName(), minio.getPassword()));
        URI endpoint = URI.create(minio.getS3URL());

        s3 = S3Client.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(creds)
                .region(Region.of("auto"))
                .forcePathStyle(true)
                .httpClient(UrlConnectionHttpClient.create())
                .build();
        presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(creds)
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();

        s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        storage = new R2ImageStorage(s3, presigner, BUCKET, Duration.ofMinutes(10));
    }

    @AfterAll
    static void stop() {
        if (presigner != null) presigner.close();
        if (s3 != null) s3.close();
        if (minio != null) minio.stop();
    }

    @Test
    void storePanel_uploadsObjectUnderPanelKey() {
        byte[] png = "fake-png-p1".getBytes();

        String key = storage.storePanel("story-1", 1, png);

        assertThat(key).isEqualTo("panels/story-1/p1.png");
        byte[] inBucket = s3.getObjectAsBytes(
                GetObjectRequest.builder().bucket(BUCKET).key(key).build()).asByteArray();
        assertThat(inBucket).isEqualTo(png);
    }

    @Test
    void urlFor_returnsPresignedUrlThatDownloadsTheBytes() throws Exception {
        byte[] png = "fake-png-p2".getBytes();
        String key = storage.storePanel("story-2", 2, png);

        String url = storage.urlFor(key);

        assertThat(url).contains("X-Amz-Signature").contains("p2.png");
        HttpResponse<byte[]> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).isEqualTo(png);
    }

    @Test
    void bucketIsPrivate_unsignedGetIsDenied() throws Exception {
        byte[] png = "secret".getBytes();
        String key = storage.storePanel("story-3", 1, png);

        String unsignedUrl = minio.getS3URL() + "/" + BUCKET + "/" + key;
        HttpResponse<byte[]> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(unsignedUrl)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(res.statusCode()).isGreaterThanOrEqualTo(400);
    }

    @Test
    void urlFor_null_returnsNull() {
        assertThat(storage.urlFor(null)).isNull();
    }

    @Test
    void deleteByKey_removesSingleObject() {
        String key = storage.storePanel("story-9", 1, "p1".getBytes());

        storage.deleteByKey(key);

        assertThat(objectExists(key)).isFalse();
    }

    @Test
    void deleteByKey_swallowsErrorsForMissingObjects() {
        // R2/S3 DELETE on a non-existent key is a no-op (204), but deleteByKey must also
        // swallow null/blank without hitting the API at all.
        storage.deleteByKey(null);
        storage.deleteByKey("");
        storage.deleteByKey("panels/story-does-not-exist/p1.png");
    }

    private boolean objectExists(String key) {
        try {
            s3.getObjectAsBytes(GetObjectRequest.builder().bucket(BUCKET).key(key).build());
            return true;
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException noSuchKeyException) {
            return false;
        }
    }
}
