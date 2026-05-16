package com.kazka.auth.token;

import com.kazka.auth.AuthProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class JwtBeans {

    @Bean
    AuthProperties.Jwt jwtProperties(AuthProperties authProperties) {
        return authProperties.jwt();
    }

    @Bean
    AuthProperties.Apple appleProperties(AuthProperties authProperties) {
        return authProperties.apple();
    }

    @Bean
    AuthProperties.Google googleProperties(AuthProperties authProperties) {
        return authProperties.google();
    }
}
