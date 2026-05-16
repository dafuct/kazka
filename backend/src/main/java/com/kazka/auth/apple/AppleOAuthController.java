package com.kazka.auth.apple;

import com.kazka.auth.AuthProperties;
import com.kazka.auth.KazkaUserDetails;
import com.kazka.auth.apple.dto.AppleLoginRequest;
import com.kazka.auth.token.RefreshTokenService;
import com.kazka.auth.token.TokenIssuer;
import com.kazka.auth.token.dto.TokenResponse;
import com.kazka.user.User;
import com.kazka.user.UserDto;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/auth/oauth")
public class AppleOAuthController {

    private final AppleIdentityTokenVerifier verifier;
    private final AppleOAuthService oauthService;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenService refreshTokens;
    private final AuthProperties props;
    private final WebSessionServerSecurityContextRepository contextRepo =
            new WebSessionServerSecurityContextRepository();

    public AppleOAuthController(AppleIdentityTokenVerifier verifier,
                                AppleOAuthService oauthService,
                                TokenIssuer tokenIssuer,
                                RefreshTokenService refreshTokens,
                                AuthProperties props) {
        this.verifier = verifier;
        this.oauthService = oauthService;
        this.tokenIssuer = tokenIssuer;
        this.refreshTokens = refreshTokens;
        this.props = props;
    }

    @PostMapping("/apple")
    public Mono<TokenResponse> apple(@RequestBody @Valid AppleLoginRequest req,
                                     ServerWebExchange exchange) {
        return Mono.fromCallable(() -> verifier.verify(req.identityToken()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(verified -> Mono.fromCallable(() -> oauthService.linkOrCreate(
                                verified.subject(),
                                req.email() != null ? req.email() : verified.email(),
                                req.fullName()))
                        .subscribeOn(Schedulers.boundedElastic()))
                .flatMap(user -> establishSession(exchange, user).thenReturn(user))
                .flatMap(this::issueTokens);
    }

    private Mono<Void> establishSession(ServerWebExchange exchange, User user) {
        var principal = new KazkaUserDetails(user);
        var token = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return contextRepo.save(exchange, new SecurityContextImpl(token));
    }

    private Mono<TokenResponse> issueTokens(User u) {
        String access = tokenIssuer.issueAccessToken(u.getId(), u.getRole());
        return refreshTokens.issue(u.getId())
                .map(refresh -> new TokenResponse(
                        access, refresh,
                        props.jwt().accessTtl().toSeconds(),
                        UserDto.from(u)));
    }
}
