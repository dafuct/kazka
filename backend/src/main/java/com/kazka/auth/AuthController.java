package com.kazka.auth;

import com.kazka.auth.dto.AuthResponse;
import com.kazka.auth.dto.PasswordResetConfirmRequest;
import com.kazka.auth.dto.PasswordResetRequestRequest;
import com.kazka.auth.dto.SignupRequest;
import com.kazka.user.UserDto;
import com.kazka.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserResolver currentUserResolver;
    private final AuthProperties props;
    private final UserRepository users;
    private final WebSessionServerSecurityContextRepository contextRepo =
            new WebSessionServerSecurityContextRepository();

    public AuthController(AuthService authService,
                          CurrentUserResolver currentUserResolver,
                          AuthProperties props,
                          UserRepository users) {
        this.authService = authService;
        this.currentUserResolver = currentUserResolver;
        this.props = props;
        this.users = users;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AuthResponse> signup(@Valid @RequestBody SignupRequest req,
                                     ServerWebExchange exchange) {
        return Mono.fromCallable(() -> authService.signup(req.email(), req.password(), req.displayName()).user())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(dto -> establishSession(exchange, dto)
                        .thenReturn(new AuthResponse(dto)));
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<AuthResponse>> me() {
        return currentUserResolver.currentUser()
                .flatMap(cu -> Mono.fromCallable(() -> authService.findCurrent(cu.userId()))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(dto -> ResponseEntity.ok(new AuthResponse(dto)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @GetMapping("/verify-email")
    public Mono<ResponseEntity<Void>> verifyEmail(@RequestParam String token) {
        return Mono.fromCallable(() -> authService.verifyEmail(token))
                .subscribeOn(Schedulers.boundedElastic())
                .map(ok -> ResponseEntity.status(HttpStatus.SEE_OTHER)
                        .location(URI.create(props.appBaseUrl() + "/verify-email?"
                                + (ok ? "ok=1" : "error=TOKEN_INVALID")))
                        .<Void>build());
    }

    @PostMapping("/verify-email/resend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> resendVerification() {
        return currentUserResolver.requireUser()
                .flatMap(cu -> Mono.fromRunnable(() -> authService.resendVerification(cu.userId()))
                        .subscribeOn(Schedulers.boundedElastic()))
                .then();
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequestRequest req) {
        return Mono.fromRunnable(() -> authService.requestPasswordReset(req.email()))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest req) {
        return Mono.fromRunnable(() -> authService.confirmPasswordReset(req.token(), req.newPassword()))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private Mono<Void> establishSession(ServerWebExchange exchange, UserDto user) {
        return Mono.fromCallable(() -> users.findById(user.id()).orElseThrow())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(entity -> {
                    var principal = new KazkaUserDetails(entity);
                    var token = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    var context = new SecurityContextImpl(token);
                    return contextRepo.save(exchange, context);
                });
    }
}
