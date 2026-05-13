package com.kazka.device;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.kazka.auth.AuthProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Configuration
public class PushNotifierConfig {

    @Bean
    public AuthProperties.Apns apnsProperties(AuthProperties props) {
        return props.apns();
    }

    @Bean
    public Optional<ApnsClient> apnsClient(AuthProperties.Apns apns) throws Exception {
        if (!Boolean.TRUE.equals(apns.enabled())) {
            return Optional.empty();
        }
        var signingKey = ApnsSigningKey.loadFromInputStream(
                new ByteArrayInputStream(apns.privateKeyPem().getBytes(StandardCharsets.UTF_8)),
                apns.teamId(),
                apns.keyId());
        ApnsClient client = new ApnsClientBuilder()
                .setApnsServer(apns.apnsHost(), 443)
                .setSigningKey(signingKey)
                .build();
        return Optional.of(client);
    }
}
