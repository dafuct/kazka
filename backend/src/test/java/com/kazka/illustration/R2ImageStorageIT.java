package com.kazka.illustration;

import com.kazka.story.Theme;
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
    void store_uploadsObjectUnderBareKey() {
        byte[] png = "fake-png-light".getBytes();

        String key = storage.store("story-1", Theme.LIGHT, png);

        assertThat(key).isEqualTo("story-1-light.png");
        byte[] inBucket = s3.getObjectAsBytes(
                GetObjectRequest.builder().bucket(BUCKET).key(key).build()).asByteArray();
        assertThat(inBucket).isEqualTo(png);
    }

    @Test
    void urlFor_returnsPresignedUrlThatDownloadsTheBytes() throws Exception {
        byte[] png = "fake-png-dark".getBytes();
        String key = storage.store("story-2", Theme.DARK, png);

        String url = storage.urlFor(key);

        assertThat(url).contains("X-Amz-Signature").contains(key);
        HttpResponse<byte[]> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).isEqualTo(png);
    }

    @Test
    void bucketIsPrivate_unsignedGetIsDenied() throws Exception {
        byte[] png = "secret".getBytes();
        String key = storage.store("story-3", Theme.LIGHT, png);

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
    void delete_removesAllVariants() {
        storage.store("story-9", Theme.LIGHT, "l".getBytes());
        storage.store("story-9", Theme.DARK, "d".getBytes());

        storage.delete("story-9");

        assertThat(objectExists("story-9-light.png")).isFalse();
        assertThat(objectExists("story-9-dark.png")).isFalse();
    }

    private boolean objectExists(String key) {
        try {
            s3.getObjectAsBytes(GetObjectRequest.builder().bucket(BUCKET).key(key).build());
            return true;
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            return false;
        }
    }
}
