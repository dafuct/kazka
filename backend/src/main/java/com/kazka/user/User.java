package com.kazka.user;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "users")
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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getGoogleSubject() { return googleSubject; }
    public void setGoogleSubject(String googleSubject) { this.googleSubject = googleSubject; }
    public String getAppleSubject() { return appleSubject; }
    public void setAppleSubject(String appleSubject) { this.appleSubject = appleSubject; }
    public String getAppleEmailRelay() { return appleEmailRelay; }
    public void setAppleEmailRelay(String appleEmailRelay) { this.appleEmailRelay = appleEmailRelay; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public Instant getSuspendedAt() { return suspendedAt; }
    public void setSuspendedAt(Instant suspendedAt) { this.suspendedAt = suspendedAt; }
    public String getSuspendedReason() { return suspendedReason; }
    public void setSuspendedReason(String suspendedReason) { this.suspendedReason = suspendedReason; }
    public String getSuspendedBy() { return suspendedBy; }
    public void setSuspendedBy(String suspendedBy) { this.suspendedBy = suspendedBy; }

    public boolean isSuspended() { return suspendedAt != null; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
