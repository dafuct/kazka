package com.kazka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Selects the image storage backend. {@code provider=filesystem} (default) serves uploads from a
 * local directory; {@code provider=r2} stores them in a private Cloudflare R2 bucket and hands the
 * browser short-lived presigned URLs.
 */
@ConfigurationProperties("kazka.storage")
public class StorageProperties {

    private String provider = "filesystem";
    private R2 r2 = new R2();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public R2 getR2() { return r2; }
    public void setR2(R2 r2) { this.r2 = r2; }

    public static class R2 {
        /** S3 API endpoint, e.g. https://<account-id>.r2.cloudflarestorage.com */
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
        /** How long a presigned GET URL stays valid. */
        private Duration presignTtl = Duration.ofHours(12);

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }

        public Duration getPresignTtl() { return presignTtl; }
        public void setPresignTtl(Duration presignTtl) { this.presignTtl = presignTtl; }
    }
}
