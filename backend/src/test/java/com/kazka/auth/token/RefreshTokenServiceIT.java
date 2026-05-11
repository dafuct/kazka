package com.kazka.auth.token;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenServiceIT extends AbstractIT {

    @Autowired RefreshTokenService service;
    @Autowired ReactiveStringRedisTemplate redis;

    @Test
    void should_persistUserId_when_issueCalled() {
        String token = service.issue("user-42").block();

        String userId = service.resolveUserId(token).block();
        assertThat(userId).isEqualTo("user-42");
    }

    @Test
    void should_return60CharOpaqueToken_when_issueCalled() {
        String token = service.issue("u").block();
        assertThat(token).hasSize(43);
        assertThat(token).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void should_revokeOldAndReturnNew_when_rotateCalled() {
        String original = service.issue("u").block();

        RefreshTokenService.RotateResult rotated = service.rotate(original).block();

        assertThat(rotated.newToken()).isNotEqualTo(original);
        assertThat(rotated.userId()).isEqualTo("u");
        assertThatThrownBy(() -> service.resolveUserId(original).block())
                .isInstanceOf(RefreshTokenService.UnknownRefreshTokenException.class);
        assertThat(service.resolveUserId(rotated.newToken()).block()).isEqualTo("u");
    }

    @Test
    void should_throw_when_rotateCalledWithUnknownToken() {
        assertThatThrownBy(() -> service.rotate("not-a-real-token").block())
                .isInstanceOf(RefreshTokenService.UnknownRefreshTokenException.class);
    }

    @Test
    void should_revokeToken_when_revokeCalled() {
        String token = service.issue("u").block();

        service.revoke(token).block();

        assertThatThrownBy(() -> service.resolveUserId(token).block())
                .isInstanceOf(RefreshTokenService.UnknownRefreshTokenException.class);
    }
}
