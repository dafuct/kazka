package com.kazka.auth;

import com.kazka.user.User;
import com.kazka.user.UserRole;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@NullMarked
public class KazkaUserDetails implements UserDetails {

    @Getter
    private final String userId;
    private final String passwordHash;
    @Getter
    private final UserRole role;
    @Getter
    private final boolean emailVerified;

    public KazkaUserDetails(User user) {
        this.userId = user.getId();
        this.passwordHash = user.getPasswordHash() == null ? "" : user.getPasswordHash();
        this.role = user.getRole();
        this.emailVerified = user.isEmailVerified();
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return userId; }
}
