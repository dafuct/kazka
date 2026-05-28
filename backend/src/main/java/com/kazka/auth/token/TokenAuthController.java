package com.kazka.auth.token;

import com.kazka.auth.AuthProperties;
import com.kazka.auth.exception.InvalidCredentialsException;
import com.kazka.auth.exception.InvalidRefreshTokenException;
import com.kazka.auth.token.dto.TokenLoginRequest;
import com.kazka.auth.token.dto.TokenLogoutRequest;
import com.kazka.auth.token.dto.TokenRefreshRequest;
import com.kazka.auth.token.dto.TokenResponse;
import com.kazka.user.User;
import com.kazka.user.UserDto;
import com.kazka.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth/token")
public class TokenAuthController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenService refreshTokens;
    private final AuthProperties props;

    @PostMapping("/refresh")
    public Mono<TokenResponse> refresh(@RequestBody @Valid TokenRefreshRequest req) {
        return refreshTokens.rotate(req.refreshToken())
                .onErrorMap(RefreshTokenService.UnknownRefreshTokenException.class,
                        e -> new InvalidRefreshTokenException())
                .flatMap(rotated -> Mono.fromCallable(() -> users.findById(rotated.userId()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(opt -> opt.orElseThrow(InvalidRefreshTokenException::new))
                        .map(u -> new TokenResponse(
                                tokenIssuer.issueAccessToken(u.getId(), u.getRole()),
                                rotated.newToken(),
                                props.jwt().accessTtl().toSeconds(),
                                UserDto.from(u))));
    }

    @PostMapping("/logout")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public Mono<Void> logout(@RequestBody @Valid TokenLogoutRequest req) {
        return refreshTokens.revoke(req.refreshToken());
    }

    @PostMapping("/login")
    public Mono<TokenResponse> login(@RequestBody @Valid TokenLoginRequest req) {
        String normalized = req.email().trim().toLowerCase();
        return Mono.fromCallable(() -> users.findByEmail(normalized))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(opt -> {
                    User user = opt.orElseThrow(InvalidCredentialsException::new);
                    if (user.getPasswordHash() == null
                            || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
                        return Mono.error(new InvalidCredentialsException());
                    }
                    String access = tokenIssuer.issueAccessToken(user.getId(), user.getRole());
                    return refreshTokens.issue(user.getId())
                            .map(refresh -> new TokenResponse(
                                    access,
                                    refresh,
                                    props.jwt().accessTtl().toSeconds(),
                                    UserDto.from(user)));
                });
    }
}
