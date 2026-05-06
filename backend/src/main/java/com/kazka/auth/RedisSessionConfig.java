package com.kazka.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisIndexedWebSession;

@Configuration
@EnableRedisIndexedWebSession
public class RedisSessionConfig {
}
