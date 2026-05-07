package com.kazka.auth;

import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler implements ServerAuthenticationSuccessHandler {

    private final UserRepository users;
    private final AuthProperties props;
    private final WebSessionServerSecurityContextRepository contextRepo =
            new WebSessionServerSecurityContextRepository();

    public OAuth2SuccessHandler(UserRepository users, AuthProperties props) {
        this.users = users;
        this.props = props;
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange exchange, Authentication auth) {
        OAuth2User oauthUser = (OAuth2User) auth.getPrincipal();
        String subject = oauthUser.getAttribute("sub");
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        return Mono.fromCallable(() -> resolveOrCreateUser(subject, email, name))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(user -> {
                    KazkaUserDetails principal = new KazkaUserDetails(user);
                    var token = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    var context = new SecurityContextImpl(token);
                    return contextRepo.save(exchange.getExchange(), context)
                            .then(redirect(exchange.getExchange(), props.appBaseUrl() + "/?auth=ok"));
                });
    }

    User resolveOrCreateUser(String subject, String email, String name) {
        var bySub = users.findByGoogleSubject(subject);
        if (bySub.isPresent()) return bySub.get();

        String normalized = email == null ? null : email.trim().toLowerCase();
        if (normalized != null) {
            var byEmail = users.findByEmail(normalized);
            if (byEmail.isPresent()) {
                User u = byEmail.get();
                if (u.getGoogleSubject() == null) {
                    u.setGoogleSubject(subject);
                    u.setEmailVerified(true);
                    return users.save(u);
                }
                return u;
            }
        }

        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(normalized);
        u.setGoogleSubject(subject);
        u.setDisplayName(name == null || name.isBlank()
                ? (normalized == null ? "user" : normalized.substring(0, normalized.indexOf('@')))
                : name);
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        return users.save(u);
    }

    private Mono<Void> redirect(ServerWebExchange exchange, String url) {
        exchange.getResponse().setStatusCode(HttpStatus.SEE_OTHER);
        exchange.getResponse().getHeaders().setLocation(URI.create(url));
        return exchange.getResponse().setComplete();
    }
}
