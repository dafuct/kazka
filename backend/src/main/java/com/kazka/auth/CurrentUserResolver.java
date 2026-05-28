package com.kazka.auth;

import com.kazka.user.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class CurrentUserResolver {

    public Mono<CurrentUser> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> Objects.requireNonNull(ctx.getAuthentication()))
                .filter(Authentication::isAuthenticated)
                .map(this::toCurrentUser);
    }

    public Mono<CurrentUser> requireUser() {
        return currentUser().switchIfEmpty(Mono.error(
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED)));
    }

    private CurrentUser toCurrentUser(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof KazkaUserDetails kud) {
            return new CurrentUser(kud.getUserId(), kud.getRole());
        }
        if (principal instanceof UserDetails ud) {
            UserRole role = ud.getAuthorities().stream()
                    .map(Object::toString)
                    .anyMatch("ROLE_ADMIN"::equals) ? UserRole.ADMIN : UserRole.USER;
            return new CurrentUser(ud.getUsername(), role);
        }
        throw new IllegalStateException("Unknown principal type: " + Objects.requireNonNull(principal).getClass());
    }

    public record CurrentUser(String userId, UserRole role) {
        public boolean isAdmin() { return role == UserRole.ADMIN; }
    }
}
