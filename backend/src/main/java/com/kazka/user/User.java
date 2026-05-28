package com.kazka.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", length = 72)
    private String passwordHash;

    @Column(name = "google_subject", length = 255)
    private String googleSubject;

    @Column(name = "apple_subject", length = 255)
    private String appleSubject;

    @Column(name = "apple_email_relay", length = 255)
    private String appleEmailRelay;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspended_reason", length = 40)
    private String suspendedReason;

    @Column(name = "suspended_by", length = 36)
    private String suspendedBy;

    @Column(name = "stories_this_month", nullable = false)
    private int storiesThisMonth;

    @Column(name = "counter_reset_at")
    private Instant counterResetAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    public boolean isSuspended() { return suspendedAt != null; }
}
