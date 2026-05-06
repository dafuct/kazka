package com.kazka.auth;

import com.kazka.user.User;
import com.kazka.user.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class KazkaUserDetails implements UserDetails {

    private final String userId;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final boolean emailVerified;

    public KazkaUserDetails(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash() == null ? "" : user.getPasswordHash();
        this.role = user.getRole();
        this.emailVerified = user.isEmailVerified();
    }

    public String getUserId() { return userId; }
    public UserRole getRole() { return role; }
    public boolean isEmailVerified() { return emailVerified; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return userId; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
