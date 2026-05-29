package com.kazka.config;

import com.kazka.illustration.FilesystemImageStorage;
import com.kazka.illustration.ImageStorage;
import com.kazka.illustration.R2ImageStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "kazka.storage.provider", havingValue = "filesystem", matchIfMissing = true)
    ImageStorage filesystemImageStorage(UploadsProperties uploads) {
        return new FilesystemImageStorage(uploads);
    }

    @Bean
    @ConditionalOnProperty(name = "kazka.storage.provider", havingValue = "r2")
    S3Client r2S3Client(StorageProperties props) {
        StorageProperties.R2 r2 = props.getR2();
        return S3Client.builder()
                .endpointOverride(URI.create(r2.getEndpoint()))
                .credentialsProvider(credentials(r2))
                .region(Region.of("auto"))
                .forcePathStyle(true)
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "kazka.storage.provider", havingValue = "r2")
    S3Presigner r2S3Presigner(StorageProperties props) {
        StorageProperties.R2 r2 = props.getR2();
        return S3Presigner.builder()
                .endpointOverride(URI.create(r2.getEndpoint()))
                .credentialsProvider(credentials(r2))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "kazka.storage.provider", havingValue = "r2")
    ImageStorage r2ImageStorage(S3Client r2S3Client, S3Presigner r2S3Presigner, StorageProperties props) {
        StorageProperties.R2 r2 = props.getR2();
        return new R2ImageStorage(r2S3Client, r2S3Presigner, r2.getBucket(), r2.getPresignTtl());
    }

    private static StaticCredentialsProvider credentials(StorageProperties.R2 r2) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(r2.getAccessKey(), r2.getSecretKey()));
    }
}
