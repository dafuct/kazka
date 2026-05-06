# Auth, Per-User Archives, and Admin — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add email/password and Google OAuth sign-in, scope every story to a user, gate creation behind email verification, and add an admin role with a "see all users" page.

**Architecture:** Spring Security on a WebFlux backend with session cookies stored in Redis (`spring-session-data-redis`); bcrypt password hashing; Spring Security OAuth2 client for Google (server-side redirect); `@SpringBootTest`+Testcontainers (MySQL, Redis) and GreenMail for tests. Frontend uses a portal-rendered `AuthModal` with three tabs, an `AuthContext` that hydrates via `GET /api/auth/me`, and a CSRF interceptor that adds `X-XSRF-TOKEN` from the cookie.

**Tech Stack:** Spring Boot 4 · Java 25 · Spring Security 7 · Spring Session 3 · Redis 7 · MySQL 8 · React 19 · TypeScript 6 · Vite 8.

**Spec:** [`docs/superpowers/specs/2026-05-03-auth-design.md`](../specs/2026-05-03-auth-design.md)

---

## File Structure

### Backend new files

```
backend/src/main/java/com/kazka/
  user/
    User.java
    UserRole.java
    UserRepository.java
    UserDto.java
    EmailVerificationToken.java
    EmailVerificationTokenRepository.java
    PasswordResetToken.java
    PasswordResetTokenRepository.java
    UserSeedRunner.java
  auth/
    AuthProperties.java
    AuthController.java
    AuthService.java
    TokenService.java
    MailService.java
    CurrentUserResolver.java
    SecurityConfig.java
    OAuth2SuccessHandler.java
    KazkaUserDetailsService.java
    SessionInvalidator.java
    dto/
      SignupRequest.java
      LoginRequest.java
      PasswordResetRequestRequest.java
      PasswordResetConfirmRequest.java
      AuthResponse.java
      ErrorResponse.java
    exception/
      EmailAlreadyExistsException.java
      EmailNotVerifiedException.java
      InvalidTokenException.java
      MailDeliveryException.java
  admin/
    AdminController.java
    AdminService.java
    AdminUserDto.java

backend/src/main/resources/mail/
  verify-email-subject.txt
  verify-email-body.txt
  password-reset-subject.txt
  password-reset-body.txt
```

### Backend modified files

```
backend/build.gradle                                          (new dependencies)
backend/src/main/resources/application.yml                    (session, mail, oauth2, kazka.auth)
backend/src/main/resources/schema.sql                         (new tables)
backend/src/main/java/com/kazka/story/Story.java              (add userId)
backend/src/main/java/com/kazka/story/StoryRepository.java    (scoped queries)
backend/src/main/java/com/kazka/story/StoryService.java       (currentUser scoping + verified gate)
backend/src/main/java/com/kazka/story/StoryController.java    (resolve current user)
backend/src/main/java/com/kazka/story/GlobalExceptionHandler.java  (new exceptions, code shape)
backend/src/test/resources/application-test.yml               (Redis test config)
backend/src/test/java/com/kazka/story/StoryRepositoryTest.java     (set userId on fixtures)
backend/src/test/java/com/kazka/story/StoryControllerTest.java     (auth + user setup)
docker-compose.yml                                            (redis service)
.env.example                                                  (auth env vars)
```

### Frontend new files

```
frontend/src/lib/
  AuthContext.tsx
  AuthModalContext.tsx
  csrf.ts
frontend/src/components/auth/
  AuthModal.tsx
  AuthModal.module.css
  SignInForm.tsx
  SignUpForm.tsx
  ForgotPasswordForm.tsx
  GoogleButton.tsx
  RequireAuth.tsx
  RequireAdmin.tsx
frontend/src/pages/
  EmailVerifiedPage.tsx
  EmailVerifiedPage.module.css
  PasswordResetPage.tsx
  PasswordResetPage.module.css
  AdminUsersPage.tsx
  AdminUsersPage.module.css
```

### Frontend modified files

```
frontend/src/lib/apiClient.ts          (credentials, CSRF, auth+admin methods)
frontend/src/lib/sseClient.ts          (credentials, CSRF)
frontend/src/lib/types.ts              (User, error codes)
frontend/src/locales/uk.ts             (auth strings)
frontend/src/locales/en.ts             (auth strings)
frontend/src/App.tsx                   (providers, routes, /?auth=ok hydration)
frontend/src/components/chrome/Nav.tsx          (sign-in/up buttons or user dropdown)
frontend/src/components/chrome/Nav.module.css   (dropdown styles)
frontend/src/components/modal/StoryModal.tsx    (verify-email panel)
frontend/src/components/modal/StoryModal.module.css  (verify panel styles)
frontend/src/pages/HomePage.tsx        (CTA via requireAuth)
frontend/src/components/home/NightCta.tsx       (CTA via requireAuth)
```

---

## Phase A — Backend foundations (deps, infra, schema, entities)

### Task 1: Add Gradle dependencies

**Files:**
- Modify: `backend/build.gradle`

- [ ] **Step 1: Add new dependencies block**

Replace the `dependencies { ... }` block in `backend/build.gradle` with:

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    implementation 'org.springframework.boot:spring-boot-starter-mail'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'
    implementation 'org.springframework.session:spring-session-data-redis'
    implementation 'com.mysql:mysql-connector-j'
    implementation 'com.fasterxml.jackson.core:jackson-databind'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'io.projectreactor:reactor-test'
    testImplementation 'org.testcontainers:junit-jupiter:1.21.3'
    testImplementation 'org.testcontainers:mysql:1.21.3'
    testImplementation 'org.testcontainers:jdbc:1.21.3'
    testImplementation 'com.redis:testcontainers-redis:2.2.2'
    testImplementation 'com.icegreen:greenmail-junit5:2.1.0'
}
```

- [ ] **Step 2: Verify it resolves**

Run: `cd backend && ./gradlew dependencies --configuration runtimeClasspath -q | head -40`
Expected: lists `spring-security`, `spring-session-data-redis`, `oauth2-client`, `mail`, `data-redis-reactive`. No errors.

- [ ] **Step 3: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add backend/build.gradle
git commit -m "build(backend): add security, oauth2, mail, redis, session deps"
```

---

### Task 2: Add Redis to docker-compose and env example

**Files:**
- Modify: `docker-compose.yml`
- Modify: `.env.example`

- [ ] **Step 1: Add `redis` service and update backend env**

Replace the entire `docker-compose.yml` with:

```yaml
services:
  mysql:
    image: mysql:8.4
    container_name: kazkar-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_USER: kazkar
      MYSQL_PASSWORD: kazkar
      MYSQL_DATABASE: kazkar
    ports:
      - "3306:3306"
    volumes:
      - mysqldata:/var/lib/mysql
      - ./backend/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/schema.sql:ro
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "kazkar", "-pkazkar"]
      interval: 5s
      timeout: 3s
      retries: 10
      start_period: 30s

  redis:
    image: redis:7-alpine
    container_name: kazkar-redis
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 10

  backend:
    build: ./backend
    environment:
      DB_URL: jdbc:mysql://mysql:3306/kazkar
      DB_USER: kazkar
      DB_PASS: kazkar
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: "6379"
      HUGGINGFACE_API_TOKEN: ${HUGGINGFACE_API_TOKEN}
      HF_TEXT_MODEL: ${HF_TEXT_MODEL:-INSAIT-Institute/MamayLM-Gemma-3-12B-IT-v1.0:featherless-ai}
      HF_EDITOR_MODEL: ${HF_EDITOR_MODEL:-lapa-llm/lapa-12b-pt:featherless-ai}
      HF_IMAGE_MODEL: ${HF_IMAGE_MODEL:-black-forest-labs/FLUX.1-schnell}
      UPLOADS_DIR: /uploads
      APP_BASE_URL: ${APP_BASE_URL:-http://localhost}
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID:-}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET:-}
      MAIL_HOST: ${MAIL_HOST:-smtp.gmail.com}
      MAIL_PORT: ${MAIL_PORT:-587}
      MAIL_USERNAME: ${MAIL_USERNAME:-}
      MAIL_PASSWORD: ${MAIL_PASSWORD:-}
      MAIL_FROM: ${MAIL_FROM:-no-reply@kazka.local}
      ADMIN_EMAIL: ${ADMIN_EMAIL:-}
      ADMIN_PASSWORD: ${ADMIN_PASSWORD:-}
      COOKIE_SECURE: ${COOKIE_SECURE:-false}
    volumes:
      - uploads:/uploads
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  mysqldata: {}
  uploads: {}
```

- [ ] **Step 2: Update `.env.example`**

Replace the entire `.env.example` with:

```
# Hugging Face — get a free token at https://huggingface.co/settings/tokens
HUGGINGFACE_API_TOKEN=hf_your_token_here
HF_TEXT_MODEL=google/gemma-3-4b-it
HF_IMAGE_MODEL=black-forest-labs/FLUX.1-schnell
HF_SVG_MODEL=Qwen/Qwen2.5-72B-Instruct
# Optional: override HF API base URLs
#HF_TEXT_BASE_URL=https://router.huggingface.co
#HF_IMAGE_BASE_URL=https://api-inference.huggingface.co

# Database
DB_URL=jdbc:mysql://mysql:3306/kazkar
DB_USER=kazkar
DB_PASS=kazkar

# Auth — base URL the frontend is reachable at; used in email links
APP_BASE_URL=http://localhost

# Google OAuth — Google Cloud Console → APIs & Services → Credentials → OAuth Client ID (Web application).
# Authorized redirect URI: ${APP_BASE_URL}/login/oauth2/code/google
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

# SMTP — easiest is a Gmail account with an app password
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=no-reply@kazka.local

# Seeded admin (created at startup if no user has this email)
ADMIN_EMAIL=admin@kazka.local
ADMIN_PASSWORD=change-me-please

# Set to true in HTTPS production environments
COOKIE_SECURE=false
```

- [ ] **Step 3: Verify compose parses**

Run: `docker compose config -q`
Expected: no output, exit code 0.

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml .env.example
git commit -m "build: add redis service + auth env vars"
```

---

### Task 3: Update application.yml

**Files:**
- Modify: `backend/src/main/resources/application.yml`

- [ ] **Step 1: Replace the file with full config**

```yaml
spring:
  application:
    name: kazkar
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/kazkar}
    username: ${DB_USER:kazkar}
    password: ${DB_PASS:kazkar}
    driver-class-name: com.mysql.cj.jdbc.Driver
  sql:
    init:
      mode: never
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.dialect: org.hibernate.dialect.MySQLDialect
    defer-datasource-initialization: true
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: ${SPRING_DATA_REDIS_PORT:6379}
  session:
    store-type: redis
    timeout: 14d
    redis:
      namespace: kazka:session
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
      mail.smtp.starttls.required: true
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:}
            client-secret: ${GOOGLE_CLIENT_SECRET:}
            scope: openid,email,profile
server:
  port: 8080
  reactive:
    session:
      cookie:
        same-site: lax
        secure: ${COOKIE_SECURE:false}
        http-only: true
kazka:
  huggingface:
    api-token: ${HUGGINGFACE_API_TOKEN:}
    text-model: ${HF_TEXT_MODEL:INSAIT-Institute/MamayLM-Gemma-3-12B-IT-v1.0:featherless-ai}
    editor-model: ${HF_EDITOR_MODEL:INSAIT-Institute/MamayLM-Gemma-3-12B-IT-v1.0:featherless-ai}
    image-model: ${HF_IMAGE_MODEL:black-forest-labs/FLUX.1-schnell}
    scene-model: ${HF_SCENE_MODEL:Qwen/Qwen2.5-72B-Instruct}
    text-base-url: ${HF_TEXT_BASE_URL:https://router.huggingface.co}
    image-base-url: ${HF_IMAGE_BASE_URL:https://router.huggingface.co}
  uploads:
    dir: ${UPLOADS_DIR:./uploads}
  auth:
    app-base-url: ${APP_BASE_URL:http://localhost}
    mail-from: ${MAIL_FROM:no-reply@kazka.local}
    token-ttl:
      email-verification: 24h
      password-reset: 1h
    admin:
      email: ${ADMIN_EMAIL:}
      password: ${ADMIN_PASSWORD:}
```

- [ ] **Step 2: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/application.yml
git commit -m "config(backend): add session redis, oauth2 google, mail, kazka.auth properties"
```

---

### Task 4: Add AuthProperties

**Files:**
- Create: `backend/src/main/java/com/kazka/auth/AuthProperties.java`

- [ ] **Step 1: Create the class**

```java
package com.kazka.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("kazka.auth")
public record AuthProperties(
        String appBaseUrl,
        String mailFrom,
        TokenTtl tokenTtl,
        Admin admin
) {
    public record TokenTtl(Duration emailVerification, Duration passwordReset) {}
    public record Admin(String email, String password) {}
}
```

- [ ] **Step 2: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL. (`@ConfigurationPropertiesScan` is already on `KazkaApplication`.)

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/auth/AuthProperties.java
git commit -m "feat(auth): add AuthProperties config bindings"
```

---

### Task 5: Schema migration — users, tokens, stories.user_id

**Files:**
- Modify: `backend/src/main/resources/schema.sql`

- [ ] **Step 1: Replace schema.sql with new layout**

```sql
DROP TABLE IF EXISTS stories;
DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS email_verification_tokens;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(72)  NULL,
    google_subject  VARCHAR(255) NULL,
    display_name    VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_google_subject (google_subject)
);

CREATE TABLE email_verification_tokens (
    token        VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id      VARCHAR(36)  NOT NULL,
    expires_at   DATETIME(3)  NOT NULL,
    consumed_at  DATETIME(3)  NULL,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_evt_user (user_id)
);

CREATE TABLE password_reset_tokens (
    token        VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id      VARCHAR(36)  NOT NULL,
    expires_at   DATETIME(3)  NOT NULL,
    consumed_at  DATETIME(3)  NULL,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_prt_user (user_id)
);

CREATE TABLE stories (
    id                       VARCHAR(36)   NOT NULL PRIMARY KEY,
    user_id                  VARCHAR(36)   NOT NULL,
    title                    TEXT          NOT NULL,
    theme                    TEXT          NOT NULL,
    characters               JSON          NOT NULL,
    age_group                VARCHAR(10)   NOT NULL,
    length                   VARCHAR(10)   NOT NULL,
    language                 VARCHAR(5)    NOT NULL DEFAULT 'uk',
    content                  LONGTEXT      NOT NULL,
    illustration_path_light  VARCHAR(500)  NULL,
    illustration_path_dark   VARCHAR(500)  NULL,
    illustration_status      VARCHAR(20)   NOT NULL DEFAULT 'pending',
    created_at               DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at               DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_stories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_stories_user_created (user_id, created_at DESC)
);
```

- [ ] **Step 2: Re-init the dev DB (manual)**

Run from project root:

```bash
docker compose down -v
docker compose up -d mysql redis
```

(Volume removal wipes the previous `mysqldata` so the new `schema.sql` runs as the initdb script. Hibernate `validate` mode requires the new schema to be present before the app boots — entities are added in the next tasks.)

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/schema.sql
git commit -m "feat(db): rebuild schema with users, verification tokens, stories.user_id"
```

---

### Task 6: User entity, role enum, repository

**Files:**
- Create: `backend/src/main/java/com/kazka/user/UserRole.java`
- Create: `backend/src/main/java/com/kazka/user/User.java`
- Create: `backend/src/main/java/com/kazka/user/UserRepository.java`

- [ ] **Step 1: UserRole enum**

```java
package com.kazka.user;

public enum UserRole { USER, ADMIN }
```

- [ ] **Step 2: User entity**

```java
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

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

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
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 3: UserRepository**

```java
package com.kazka.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleSubject(String googleSubject);
    boolean existsByEmail(String email);
    List<User> findAllByOrderByCreatedAtDesc();
}
```

- [ ] **Step 4: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/kazka/user/User.java backend/src/main/java/com/kazka/user/UserRole.java backend/src/main/java/com/kazka/user/UserRepository.java
git commit -m "feat(user): add User entity, UserRole, repository"
```

---

### Task 7: EmailVerificationToken entity + repository

**Files:**
- Create: `backend/src/main/java/com/kazka/user/EmailVerificationToken.java`
- Create: `backend/src/main/java/com/kazka/user/EmailVerificationTokenRepository.java`

- [ ] **Step 1: Entity**

```java
package com.kazka.user;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

    @Id
    @Column(length = 64)
    private String token;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 2: Repository**

```java
package com.kazka.user;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, String> {

    @Modifying
    @Transactional
    @Query("update EmailVerificationToken t set t.consumedAt = :now " +
           "where t.userId = :userId and t.consumedAt is null")
    int consumeAllByUserId(@Param("userId") String userId, @Param("now") Instant now);
}
```

- [ ] **Step 3: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kazka/user/EmailVerificationToken.java backend/src/main/java/com/kazka/user/EmailVerificationTokenRepository.java
git commit -m "feat(user): add EmailVerificationToken entity + repo"
```

---

### Task 8: PasswordResetToken entity + repository

**Files:**
- Create: `backend/src/main/java/com/kazka/user/PasswordResetToken.java`
- Create: `backend/src/main/java/com/kazka/user/PasswordResetTokenRepository.java`

- [ ] **Step 1: Entity**

```java
package com.kazka.user;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @Column(length = 64)
    private String token;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 2: Repository**

```java
package com.kazka.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
}
```

- [ ] **Step 3: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kazka/user/PasswordResetToken.java backend/src/main/java/com/kazka/user/PasswordResetTokenRepository.java
git commit -m "feat(user): add PasswordResetToken entity + repo"
```

---

### Task 9: Story.userId + scoped repository methods

**Files:**
- Modify: `backend/src/main/java/com/kazka/story/Story.java`
- Modify: `backend/src/main/java/com/kazka/story/StoryRepository.java`

- [ ] **Step 1: Add `userId` to Story entity**

In `Story.java`, after the existing `private String id;` field, insert:

```java
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
```

And add getter/setter near the other accessors:

```java
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
```

- [ ] **Step 2: Add scoped methods to StoryRepository**

Replace `StoryRepository.java` with:

```java
package com.kazka.story;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoryRepository extends JpaRepository<Story, String> {
    Page<Story> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Story> findAllByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Optional<Story> findByIdAndUserId(String id, String userId);
    long countByUserId(String userId);
}
```

- [ ] **Step 3: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kazka/story/Story.java backend/src/main/java/com/kazka/story/StoryRepository.java
git commit -m "feat(story): add userId field + scoped repo queries"
```

---

## Phase B — Domain services

### Task 10: TokenService

**Files:**
- Create: `backend/src/main/java/com/kazka/auth/TokenService.java`

- [ ] **Step 1: Implementation**

```java
package com.kazka.auth;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    public String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
```

- [ ] **Step 2: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/auth/TokenService.java
git commit -m "feat(auth): add TokenService for URL-safe random tokens"
```

---

### Task 11: TokenServiceTest

**Files:**
- Create: `backend/src/test/java/com/kazka/auth/TokenServiceTest.java`

- [ ] **Step 1: Write failing test**

```java
package com.kazka.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private final TokenService tokenService = new TokenService();

    @Test
    void should_returnUrlSafeToken_when_generate() {
        String token = tokenService.generate();

        assertThat(token).hasSize(43);
        assertThat(token).matches("^[A-Za-z0-9_-]+$");
    }

    @Test
    void should_returnDifferentTokens_when_generateCalledTwice() {
        assertThat(tokenService.generate()).isNotEqualTo(tokenService.generate());
    }
}
```

- [ ] **Step 2: Run test**

Run: `cd backend && ./gradlew test --tests "com.kazka.auth.TokenServiceTest" -q`
Expected: PASS (TokenService already exists from Task 10).

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/kazka/auth/TokenServiceTest.java
git commit -m "test(auth): TokenService generates 43-char URL-safe tokens"
```

---

### Task 12: Auth domain exceptions

**Files:**
- Create: `backend/src/main/java/com/kazka/auth/exception/EmailAlreadyExistsException.java`
- Create: `backend/src/main/java/com/kazka/auth/exception/EmailNotVerifiedException.java`
- Create: `backend/src/main/java/com/kazka/auth/exception/InvalidTokenException.java`
- Create: `backend/src/main/java/com/kazka/auth/exception/MailDeliveryException.java`

- [ ] **Step 1: EmailAlreadyExistsException**

```java
package com.kazka.auth.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException() {
        super("Email already exists");
    }
}
```

- [ ] **Step 2: EmailNotVerifiedException**

```java
package com.kazka.auth.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Email is not verified");
    }
}
```

- [ ] **Step 3: InvalidTokenException**

```java
package com.kazka.auth.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Token is missing, expired, or already used");
    }
}
```

- [ ] **Step 4: MailDeliveryException**

```java
package com.kazka.auth.exception;

public class MailDeliveryException extends RuntimeException {
    public MailDeliveryException(Throwable cause) {
        super("Failed to send email", cause);
    }
}
```

- [ ] **Step 5: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/kazka/auth/exception/
git commit -m "feat(auth): add domain exceptions"
```

---

### Task 13: Mail templates

**Files:**
- Create: `backend/src/main/resources/mail/verify-email-subject.txt`
- Create: `backend/src/main/resources/mail/verify-email-body.txt`
- Create: `backend/src/main/resources/mail/password-reset-subject.txt`
- Create: `backend/src/main/resources/mail/password-reset-body.txt`

- [ ] **Step 1: verify-email-subject.txt**

```
Confirm your Kazka email
```

- [ ] **Step 2: verify-email-body.txt**

```
Hi {displayName},

Welcome to Kazka. Please confirm your email by clicking the link below:

{baseUrl}/api/auth/verify-email?token={token}

The link expires in 24 hours.

If you didn't create an account, you can safely ignore this message.

— Kazka
```

- [ ] **Step 3: password-reset-subject.txt**

```
Reset your Kazka password
```

- [ ] **Step 4: password-reset-body.txt**

```
Hi {displayName},

We received a request to reset your Kazka password. Click the link below to choose a new one:

{baseUrl}/reset-password?token={token}

The link expires in 1 hour.

If you didn't request a reset, you can ignore this email — your password won't change.

— Kazka
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/mail/
git commit -m "feat(auth): add plain-text mail templates"
```

---

### Task 14: MailService

**Files:**
- Create: `backend/src/main/java/com/kazka/auth/MailService.java`

- [ ] **Step 1: Implementation**

```java
package com.kazka.auth;

import com.kazka.auth.exception.MailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final AuthProperties props;

    public MailService(JavaMailSender mailSender, AuthProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    public void sendVerificationEmail(String to, String displayName, String token) {
        send(to, "mail/verify-email-subject.txt", "mail/verify-email-body.txt",
                Map.of("displayName", displayName, "token", token, "baseUrl", props.appBaseUrl()));
    }

    public void sendPasswordResetEmail(String to, String displayName, String token) {
        send(to, "mail/password-reset-subject.txt", "mail/password-reset-body.txt",
                Map.of("displayName", displayName, "token", token, "baseUrl", props.appBaseUrl()));
    }

    private void send(String to, String subjectPath, String bodyPath, Map<String, String> vars) {
        try {
            String subject = render(subjectPath, vars).trim();
            String body = render(bodyPath, vars);
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.mailFrom());
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read mail template " + subjectPath, e);
        } catch (org.springframework.mail.MailException e) {
            log.warn("Failed to send mail to {}: {}", to, e.getMessage());
            throw new MailDeliveryException(e);
        }
    }

    private String render(String resourcePath, Map<String, String> vars) throws IOException {
        try (var in = new ClassPathResource(resourcePath).getInputStream()) {
            String template = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            for (var entry : vars.entrySet()) {
                template = template.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            return template;
        }
    }
}
```

- [ ] **Step 2: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/auth/MailService.java
git commit -m "feat(auth): add MailService with classpath template rendering"
```

---

### Task 15: UserDto record

**Files:**
- Create: `backend/src/main/java/com/kazka/user/UserDto.java`

- [ ] **Step 1: Record + factory**

```java
package com.kazka.user;

public record UserDto(
        String id,
        String email,
        String displayName,
        UserRole role,
        boolean emailVerified,
        boolean googleLinked
) {
    public static UserDto from(User u) {
        return new UserDto(
                u.getId(),
                u.getEmail(),
                u.getDisplayName(),
                u.getRole(),
                u.isEmailVerified(),
                u.getGoogleSubject() != null
        );
    }
}
```

- [ ] **Step 2: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/user/UserDto.java
git commit -m "feat(user): add UserDto record"
```

---

### Task 16: AuthService — signup

**Files:**
- Create: `backend/src/main/java/com/kazka/auth/AuthService.java`

- [ ] **Step 1: Skeleton with `signup(...)`**

```java
package com.kazka.auth;

import com.kazka.auth.exception.EmailAlreadyExistsException;
import com.kazka.user.EmailVerificationToken;
import com.kazka.user.EmailVerificationTokenRepository;
import com.kazka.user.User;
import com.kazka.user.UserDto;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final EmailVerificationTokenRepository emailTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final MailService mailService;
    private final AuthProperties props;

    public AuthService(UserRepository users,
                       EmailVerificationTokenRepository emailTokens,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       MailService mailService,
                       AuthProperties props) {
        this.users = users;
        this.emailTokens = emailTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.props = props;
    }

    @Transactional
    public UserDto signup(String email, String password, String displayName) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmail(normalized)) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(normalized);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName.trim());
        user.setRole(UserRole.USER);
        user.setEmailVerified(false);
        users.save(user);

        sendVerification(user);
        return UserDto.from(user);
    }

    private void sendVerification(User user) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(tokenService.generate());
        token.setUserId(user.getId());
        token.setExpiresAt(Instant.now().plus(props.tokenTtl().emailVerification()));
        emailTokens.save(token);

        try {
            mailService.sendVerificationEmail(user.getEmail(), user.getDisplayName(), token.getToken());
        } catch (Exception e) {
            log.warn("Verification email failed for {}; user can resend later", user.getEmail());
        }
    }
}
```

- [ ] **Step 2: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL. (`PasswordEncoder` bean is added in `SecurityConfig` later; if compilation fails because of missing bean elsewhere, that's expected — only Spring runtime cares.)

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/auth/AuthService.java
git commit -m "feat(auth): AuthService.signup with bcrypt + verification email"
```

---

### Task 17: AuthService — verify email + resend

**Files:**
- Modify: `backend/src/main/java/com/kazka/auth/AuthService.java`

- [ ] **Step 1: Add `verifyEmail(String token)` method**

Add inside the `AuthService` class:

```java
    @Transactional
    public boolean verifyEmail(String token) {
        var opt = emailTokens.findById(token);
        if (opt.isEmpty()) return false;
        EmailVerificationToken evt = opt.get();
        if (evt.getConsumedAt() != null) return false;
        if (evt.getExpiresAt().isBefore(Instant.now())) return false;

        User user = users.findById(evt.getUserId()).orElse(null);
        if (user == null) return false;

        user.setEmailVerified(true);
        users.save(user);

        evt.setConsumedAt(Instant.now());
        emailTokens.save(evt);
        return true;
    }

    @Transactional
    public void resendVerification(String userId) {
        User user = users.findById(userId).orElseThrow();
        if (user.isEmailVerified()) return;
        emailTokens.consumeAllByUserId(userId, Instant.now());
        sendVerification(user);
    }
```

- [ ] **Step 2: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/auth/AuthService.java
git commit -m "feat(auth): AuthService.verifyEmail + resendVerification"
```

---

### Task 18: AuthService — password reset request + confirm

**Files:**
- Modify: `backend/src/main/java/com/kazka/auth/AuthService.java`

- [ ] **Step 1: Add fields and constructor params**

Replace the constructor and the field block at the top of `AuthService` with:

```java
    private final UserRepository users;
    private final EmailVerificationTokenRepository emailTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final MailService mailService;
    private final SessionInvalidator sessionInvalidator;
    private final AuthProperties props;

    public AuthService(UserRepository users,
                       EmailVerificationTokenRepository emailTokens,
                       PasswordResetTokenRepository resetTokens,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       MailService mailService,
                       SessionInvalidator sessionInvalidator,
                       AuthProperties props) {
        this.users = users;
        this.emailTokens = emailTokens;
        this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.sessionInvalidator = sessionInvalidator;
        this.props = props;
    }
```

Add this import at the top:

```java
import com.kazka.user.PasswordResetToken;
import com.kazka.user.PasswordResetTokenRepository;
```

- [ ] **Step 2: Add `requestPasswordReset` and `confirmPasswordReset`**

```java
    @Transactional
    public void requestPasswordReset(String email) {
        String normalized = email.trim().toLowerCase();
        var userOpt = users.findByEmail(normalized);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();
        if (user.getPasswordHash() == null) return;

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(tokenService.generate());
        token.setUserId(user.getId());
        token.setExpiresAt(Instant.now().plus(props.tokenTtl().passwordReset()));
        resetTokens.save(token);

        try {
            mailService.sendPasswordResetEmail(user.getEmail(), user.getDisplayName(), token.getToken());
        } catch (Exception e) {
            log.warn("Password reset email failed for {}", user.getEmail());
        }
    }

    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        var opt = resetTokens.findById(token);
        if (opt.isEmpty()) throw new com.kazka.auth.exception.InvalidTokenException();
        PasswordResetToken prt = opt.get();
        if (prt.getConsumedAt() != null) throw new com.kazka.auth.exception.InvalidTokenException();
        if (prt.getExpiresAt().isBefore(Instant.now())) throw new com.kazka.auth.exception.InvalidTokenException();

        User user = users.findById(prt.getUserId())
                .orElseThrow(com.kazka.auth.exception.InvalidTokenException::new);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);

        prt.setConsumedAt(Instant.now());
        resetTokens.save(prt);

        sessionInvalidator.invalidateAllForUser(user.getId());
    }
```

- [ ] **Step 3: Add `findById` lookup helper**

Below the methods above add:

```java
    public UserDto findCurrent(String userId) {
        return users.findById(userId).map(UserDto::from).orElseThrow();
    }
```

- [ ] **Step 4: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: fails with "cannot find symbol SessionInvalidator". This is intentional — `SessionInvalidator` is added in Task 19.

- [ ] **Step 5: Commit (deferred — bundled with Task 19)**

Do not commit yet.

---

### Task 19: SessionInvalidator (Redis-backed)

**Files:**
- Create: `backend/src/main/java/com/kazka/auth/SessionInvalidator.java`

- [ ] **Step 1: Create the helper**

```java
package com.kazka.auth;

import org.springframework.session.ReactiveFindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class SessionInvalidator {

    private final ReactiveFindByIndexNameSessionRepository<? extends Session> sessions;

    public SessionInvalidator(ReactiveFindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    public void invalidateAllForUser(String userId) {
        sessions.findByPrincipalName(userId)
                .flatMapMany(map -> Flux.fromIterable(map.keySet()))
                .flatMap(sessions::deleteById)
                .blockLast();
    }
}
```

- [ ] **Step 2: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL (AuthService now resolves).

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/auth/AuthService.java backend/src/main/java/com/kazka/auth/SessionInvalidator.java
git commit -m "feat(auth): password reset flow + Redis session invalidation"
```

---

### Task 20: CurrentUserResolver

**Files:**
- Create: `backend/src/main/java/com/kazka/auth/CurrentUserResolver.java`

- [ ] **Step 1: Helper that returns the authenticated user id+role**

```java
package com.kazka.auth;

import com.kazka.user.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CurrentUserResolver {

    public Mono<CurrentUser> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
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
        throw new IllegalStateException("Unknown principal type: " + principal.getClass());
    }

    public record CurrentUser(String userId, UserRole role) {
        public boolean isAdmin() { return role == UserRole.ADMIN; }
    }
}
```

- [ ] **Step 2: Compile (will fail until KazkaUserDetails exists in Task 21)**

Run: `cd backend && ./gradlew compileJava -q`
Expected: fails with "cannot find symbol KazkaUserDetails". Continue to Task 21.

---

### Task 21: KazkaUserDetails + ReactiveUserDetailsService

**Files:**
- Create: `backend/src/main/java/com/kazka/auth/KazkaUserDetails.java`
- Create: `backend/src/main/java/com/kazka/auth/KazkaUserDetailsService.java`

- [ ] **Step 1: KazkaUserDetails**

```java
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
```

- [ ] **Step 2: KazkaUserDetailsService**

```java
package com.kazka.auth;

import com.kazka.user.UserRepository;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class KazkaUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository users;

    public KazkaUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public Mono<UserDetails> findByUsername(String email) {
        return Mono.fromCallable(() -> users.findByEmail(email.trim().toLowerCase())
                        .map(KazkaUserDetails::new)
                        .orElseThrow(() -> new UsernameNotFoundException(email)))
                .subscribeOn(Schedulers.boundedElastic())
                .cast(UserDetails.class);
    }
}
```

- [ ] **Step 3: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL (CurrentUserResolver now resolves).

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kazka/auth/CurrentUserResolver.java backend/src/main/java/com/kazka/auth/KazkaUserDetails.java backend/src/main/java/com/kazka/auth/KazkaUserDetailsService.java
git commit -m "feat(auth): CurrentUserResolver + reactive UserDetailsService"
```

---

## Phase C — Spring Security wiring

### Task 22: SecurityConfig

**Files:**
- Create: `backend/src/main/java/com/kazka/auth/SecurityConfig.java`

- [ ] **Step 1: Create SecurityConfig**

```java
package com.kazka.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsRepositoryReactiveAuthenticationManager authenticationManager(
            ReactiveUserDetailsService uds, PasswordEncoder pe) {
        var mgr = new UserDetailsRepositoryReactiveAuthenticationManager(uds);
        mgr.setPasswordEncoder(pe);
        return mgr;
    }

    @Bean
    public SecurityWebFilterChain securityFilterChain(
            ServerHttpSecurity http,
            UserDetailsRepositoryReactiveAuthenticationManager authManager,
            OAuth2SuccessHandler oauthSuccess,
            ObjectMapper objectMapper) {

        var loginFilter = new AuthenticationWebFilter(authManager);
        loginFilter.setRequiresAuthenticationMatcher(
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/api/auth/login"));
        loginFilter.setServerAuthenticationConverter(new JsonLoginConverter(objectMapper));
        loginFilter.setAuthenticationSuccessHandler(new JsonLoginSuccessHandler(objectMapper));
        loginFilter.setAuthenticationFailureHandler((webFilterExchange, exception) ->
                writeError(webFilterExchange.getExchange(), HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", objectMapper));

        return http
                .cors(c -> c.configurationSource(corsConfigSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                        .requireCsrfProtectionMatcher(new AndServerWebExchangeMatcher(
                                CsrfWebFilter.DEFAULT_CSRF_MATCHER,
                                new NegatedServerWebExchangeMatcher(
                                        ServerWebExchangeMatchers.pathMatchers(
                                                "/oauth2/**", "/login/oauth2/**")))))
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .pathMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/logout",
                                "/api/auth/password-reset/request",
                                "/api/auth/password-reset/confirm").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/auth/me", "/api/auth/verify-email").permitAll()
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().permitAll())
                .addFilterAt(loginFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .oauth2Login(o -> o.authenticationSuccessHandler(oauthSuccess))
                .logout(l -> l.logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((webFilterExchange, authentication) -> {
                            webFilterExchange.getExchange().getResponse().setStatusCode(HttpStatus.NO_CONTENT);
                            return Mono.empty();
                        }))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((swe, ex) ->
                                writeError(swe, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", objectMapper))
                        .accessDeniedHandler((swe, ex) ->
                                writeError(swe, HttpStatus.FORBIDDEN, "FORBIDDEN", objectMapper)))
                .formLogin(Customizer.withDefaults())
                .httpBasic(b -> b.disable())
                .build();
    }

    @Bean
    public WebFilter csrfTokenAttributeFilter() {
        return (exchange, chain) -> {
            Mono<CsrfToken> token = exchange.getAttribute(CsrfToken.class.getName());
            if (token != null) return token.then(chain.filter(exchange));
            return chain.filter(exchange);
        };
    }

    @Profile("dev")
    @Bean
    public CorsConfigurationSource corsConfigSource() {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:5173"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        var src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    private static Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status,
                                         String code, ObjectMapper mapper) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        try {
            byte[] body = mapper.writeValueAsBytes(java.util.Map.of("error", code));
            return exchange.getResponse().writeWith(Mono.just(
                    exchange.getResponse().bufferFactory().wrap(body)));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
```

- [ ] **Step 2: Notes for `JsonLoginConverter` and `JsonLoginSuccessHandler`**

These two helpers are added in Task 23 (next), so the project will not compile yet.

---

### Task 23: JSON login converter + success handler

**Files:**
- Create: `backend/src/main/java/com/kazka/auth/JsonLoginConverter.java`
- Create: `backend/src/main/java/com/kazka/auth/JsonLoginSuccessHandler.java`

- [ ] **Step 1: JsonLoginConverter**

```java
package com.kazka.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class JsonLoginConverter implements ServerAuthenticationConverter {

    private final ObjectMapper mapper;

    JsonLoginConverter(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return exchange.getRequest().getBody()
                .reduce(new java.io.ByteArrayOutputStream(), (out, buf) -> {
                    out.writeBytes(buf.asInputStream().readAllBytes());
                    return out;
                })
                .map(out -> {
                    try {
                        var node = mapper.readTree(out.toByteArray());
                        String email = node.path("email").asText("").trim().toLowerCase();
                        String password = node.path("password").asText("");
                        return (Authentication) new UsernamePasswordAuthenticationToken(email, password);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("invalid login body");
                    }
                });
    }
}
```

- [ ] **Step 2: JsonLoginSuccessHandler**

```java
package com.kazka.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.user.UserDto;
import com.kazka.user.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

class JsonLoginSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final ObjectMapper mapper;
    private UserRepository users;

    JsonLoginSuccessHandler(ObjectMapper mapper) { this.mapper = mapper; }

    void setUsers(UserRepository users) { this.users = users; }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange exchange, Authentication auth) {
        KazkaUserDetails kud = (KazkaUserDetails) auth.getPrincipal();
        return Mono.fromCallable(() -> users.findById(kud.getUserId()).orElseThrow())
                .subscribeOn(Schedulers.boundedElastic())
                .map(UserDto::from)
                .flatMap(dto -> writeBody(exchange, dto));
    }

    private Mono<Void> writeBody(WebFilterExchange exchange, UserDto dto) {
        var resp = exchange.getExchange().getResponse();
        resp.setStatusCode(HttpStatus.OK);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = mapper.writeValueAsBytes(java.util.Map.of("user", dto));
            return resp.writeWith(Mono.just(resp.bufferFactory().wrap(body)));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
```

- [ ] **Step 3: Wire UserRepository into the success handler from SecurityConfig**

In `SecurityConfig.securityFilterChain(...)`, change the `loginFilter` block: take a new `UserRepository` constructor parameter and call `successHandler.setUsers(userRepository)`. Update the bean signature to:

```java
    @Bean
    public SecurityWebFilterChain securityFilterChain(
            ServerHttpSecurity http,
            UserDetailsRepositoryReactiveAuthenticationManager authManager,
            OAuth2SuccessHandler oauthSuccess,
            ObjectMapper objectMapper,
            com.kazka.user.UserRepository userRepository) {

        var loginFilter = new AuthenticationWebFilter(authManager);
        loginFilter.setRequiresAuthenticationMatcher(
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/api/auth/login"));
        loginFilter.setServerAuthenticationConverter(new JsonLoginConverter(objectMapper));
        var success = new JsonLoginSuccessHandler(objectMapper);
        success.setUsers(userRepository);
        loginFilter.setAuthenticationSuccessHandler(success);
        loginFilter.setAuthenticationFailureHandler((webFilterExchange, exception) ->
                writeError(webFilterExchange.getExchange(), HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", objectMapper));
        // ...rest unchanged
```

- [ ] **Step 4: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/kazka/auth/SecurityConfig.java backend/src/main/java/com/kazka/auth/JsonLoginConverter.java backend/src/main/java/com/kazka/auth/JsonLoginSuccessHandler.java
git commit -m "feat(auth): SecurityConfig with JSON login filter, CSRF, OAuth2 hook"
```

---

### Task 24: OAuth2SuccessHandler

**Files:**
- Create: `backend/src/main/java/com/kazka/auth/OAuth2SuccessHandler.java`

- [ ] **Step 1: Implementation**

```java
package com.kazka.auth;

import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
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
                .flatMap(user -> redirect(exchange.getExchange(), props.appBaseUrl() + "/?auth=ok"));
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
```

- [ ] **Step 2: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/auth/OAuth2SuccessHandler.java
git commit -m "feat(auth): OAuth2SuccessHandler — find by sub, link by email, or create"
```

---

### Task 25: Spring Session Redis enable

**Files:**
- Create: `backend/src/main/java/com/kazka/auth/RedisSessionConfig.java`

- [ ] **Step 1: Create config**

```java
package com.kazka.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisIndexedWebSession;

@Configuration
@EnableRedisIndexedWebSession
public class RedisSessionConfig {
}
```

- [ ] **Step 2: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/auth/RedisSessionConfig.java
git commit -m "feat(auth): enable Redis-indexed WebFlux sessions"
```

---

## Phase D — Auth API

### Task 26: Auth DTOs

**Files:**
- Create: `backend/src/main/java/com/kazka/auth/dto/SignupRequest.java`
- Create: `backend/src/main/java/com/kazka/auth/dto/PasswordResetRequestRequest.java`
- Create: `backend/src/main/java/com/kazka/auth/dto/PasswordResetConfirmRequest.java`
- Create: `backend/src/main/java/com/kazka/auth/dto/AuthResponse.java`

- [ ] **Step 1: SignupRequest**

```java
package com.kazka.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(min = 1, max = 100) String displayName
) {}
```

- [ ] **Step 2: PasswordResetRequestRequest**

```java
package com.kazka.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequestRequest(@Email @NotBlank String email) {}
```

- [ ] **Step 3: PasswordResetConfirmRequest**

```java
package com.kazka.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {}
```

- [ ] **Step 4: AuthResponse**

```java
package com.kazka.auth.dto;

import com.kazka.user.UserDto;

public record AuthResponse(UserDto user) {}
```

- [ ] **Step 5: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/kazka/auth/dto/
git commit -m "feat(auth): request and response DTOs"
```

---

### Task 27: AuthController

**Files:**
- Create: `backend/src/main/java/com/kazka/auth/AuthController.java`

- [ ] **Step 1: Controller**

```java
package com.kazka.auth;

import com.kazka.auth.dto.AuthResponse;
import com.kazka.auth.dto.PasswordResetConfirmRequest;
import com.kazka.auth.dto.PasswordResetRequestRequest;
import com.kazka.auth.dto.SignupRequest;
import com.kazka.user.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
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
    private final WebSessionServerSecurityContextRepository contextRepo =
            new WebSessionServerSecurityContextRepository();

    public AuthController(AuthService authService,
                          CurrentUserResolver currentUserResolver,
                          AuthProperties props) {
        this.authService = authService;
        this.currentUserResolver = currentUserResolver;
        this.props = props;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AuthResponse> signup(@Valid @RequestBody SignupRequest req,
                                     ServerWebExchange exchange) {
        return Mono.fromCallable(() -> authService.signup(req.email(), req.password(), req.displayName()))
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
                        .build());
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
        var token = new UsernamePasswordAuthenticationToken(
                new SignupPrincipal(user.id(), user.role().name()),
                null,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_" + user.role().name())));
        var context = new SecurityContextImpl(token);
        return contextRepo.save(exchange, context);
    }

    private record SignupPrincipal(String userId, String role) {}
}
```

- [ ] **Step 2: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/auth/AuthController.java
git commit -m "feat(auth): AuthController with signup/me/verify/password-reset"
```

---

### Task 28: GlobalExceptionHandler — error code shape + new types

**Files:**
- Modify: `backend/src/main/java/com/kazka/story/GlobalExceptionHandler.java`

- [ ] **Step 1: Replace handler with code-shape responses**

```java
package com.kazka.story;

import com.kazka.auth.exception.EmailAlreadyExistsException;
import com.kazka.auth.exception.EmailNotVerifiedException;
import com.kazka.auth.exception.InvalidTokenException;
import com.kazka.auth.exception.MailDeliveryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", codeFor(ex));
        if (ex.getReason() != null) body.put("message", ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_CREDENTIALS"));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailTaken(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "EMAIL_TAKEN"));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleNotVerified(EmailNotVerifiedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "EMAIL_NOT_VERIFIED"));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "TOKEN_INVALID"));
    }

    @ExceptionHandler(MailDeliveryException.class)
    public ResponseEntity<Map<String, Object>> handleMailFailure(MailDeliveryException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "MAIL_SEND_FAILED"));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(WebExchangeBindException ex) {
        Map<String, String> fields = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fields.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "VALIDATION", "fields", fields));
    }

    private static String codeFor(ResponseStatusException ex) {
        int s = ex.getStatusCode().value();
        if (s == 404) return "NOT_FOUND";
        if (s == 401) return "UNAUTHENTICATED";
        if (s == 403) return "FORBIDDEN";
        return "ERROR";
    }
}
```

- [ ] **Step 2: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/story/GlobalExceptionHandler.java
git commit -m "feat(error): error code shape + auth exception handlers"
```

---

## Phase E — Story scoping

### Task 29: StoryService — userId scoping + verified gate

**Files:**
- Modify: `backend/src/main/java/com/kazka/story/StoryService.java`

- [ ] **Step 1: Replace the file**

```java
package com.kazka.story;

import com.kazka.auth.CurrentUserResolver.CurrentUser;
import com.kazka.auth.exception.EmailNotVerifiedException;
import com.kazka.hf.HuggingFaceClient;
import com.kazka.illustration.IllustrationService;
import com.kazka.story.dto.*;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Service
public class StoryService {

    private final StoryRepository repository;
    private final UserRepository users;
    private final HuggingFaceClient hfClient;
    private final PromptBuilder promptBuilder;
    private final IllustrationService illustrationService;

    public StoryService(StoryRepository repository, UserRepository users,
                        HuggingFaceClient hfClient, PromptBuilder promptBuilder,
                        IllustrationService illustrationService) {
        this.repository = repository;
        this.users = users;
        this.hfClient = hfClient;
        this.promptBuilder = promptBuilder;
        this.illustrationService = illustrationService;
    }

    public Flux<SseEvent> generate(GenerationRequest req, CurrentUser currentUser) {
        return ensureVerified(currentUser)
                .thenMany(Flux.defer(() -> generateInternal(req, currentUser.userId())));
    }

    private Flux<SseEvent> generateInternal(GenerationRequest req, String userId) {
        String id = UUID.randomUUID().toString();
        String storySystem = promptBuilder.buildStorySystem();
        String storyUser = promptBuilder.buildStoryUserMessage(req);
        String editorSystem = promptBuilder.buildEditorSystem(req.language());

        Story story = new Story();
        story.setId(id);
        story.setUserId(userId);
        story.setTitle("");
        story.setTheme(req.theme());
        story.setCharacters(req.characters());
        story.setAgeGroup(req.ageGroup());
        story.setLength(req.length());
        story.setLanguage(req.language());
        story.setContent("");
        story.setIllustrationStatus(IllustrationStatus.PENDING);

        return Mono.fromCallable(() -> repository.save(story))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(saved -> {
                    Flux<SseEvent> meta = Flux.just(SseEvent.meta(id));
                    StringBuilder rawBuffer = new StringBuilder();
                    Flux<SseEvent> tokens = hfClient.streamText(storySystem, storyUser)
                            .doOnNext(rawBuffer::append)
                            .map(SseEvent::token)
                            .concatWith(Mono.defer(() ->
                                hfClient.streamEdit(editorSystem, rawBuffer.toString())
                                        .reduce("", String::concat)
                                        .flatMap(corrected -> Mono.fromCallable(() -> {
                                            String[] lines = corrected.split("\n");
                                            String title = "";
                                            int storyStart = 0;
                                            for (int i = 0; i < lines.length; i++) {
                                                String l = lines[i].strip();
                                                if (l.isEmpty()) continue;
                                                if (looksLikeTitle(l)) {
                                                    title = l;
                                                    storyStart = i + 1;
                                                } else {
                                                    title = req.theme();
                                                    storyStart = i;
                                                }
                                                break;
                                            }
                                            while (storyStart < lines.length && lines[storyStart].strip().isEmpty()) storyStart++;
                                            String body = String.join("\n", java.util.Arrays.copyOfRange(lines, storyStart, lines.length));
                                            saved.setTitle(title);
                                            saved.setContent(body);
                                            repository.save(saved);
                                            return SseEvent.done(id, title);
                                        }).subscribeOn(Schedulers.boundedElastic()))
                            ))
                            .onErrorResume(e -> Flux.just(SseEvent.error(e.getMessage())));
                    return meta.concatWith(tokens);
                });
    }

    private static boolean looksLikeTitle(String line) {
        if (line.length() > 60) return false;
        if (line.contains(". ") || line.contains("! ") || line.contains("? ")) return false;
        if (line.endsWith(".") || line.endsWith("!") || line.endsWith("?")) return false;
        return line.split("\\s+").length <= 6;
    }

    public Mono<Void> illustrate(String id, CurrentUser currentUser) {
        return ensureVerified(currentUser)
                .then(Mono.fromCallable(() -> findOwned(id, currentUser))
                        .subscribeOn(Schedulers.boundedElastic()))
                .then(illustrationService.generateAndStore(id)
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<PageResponse<StoryDto>> list(int page, int size, CurrentUser currentUser) {
        return Mono.fromCallable(() -> {
            Page<Story> p = currentUser.isAdmin()
                    ? repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                    : repository.findAllByUserIdOrderByCreatedAtDesc(currentUser.userId(), PageRequest.of(page, size));
            return new PageResponse<>(
                    p.getContent().stream().map(StoryDto::from).toList(),
                    p.getNumber(), p.getSize(), p.getTotalElements());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<StoryDto> findById(String id, CurrentUser currentUser) {
        return Mono.fromCallable(() -> findOwned(id, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .map(StoryDto::from);
    }

    public Mono<StoryDto> update(String id, UpdateStoryRequest req, CurrentUser currentUser) {
        return Mono.fromCallable(() -> {
            Story story = findOwned(id, currentUser);
            story.setTitle(req.title());
            story.setContent(req.content());
            return repository.save(story);
        }).subscribeOn(Schedulers.boundedElastic()).map(StoryDto::from);
    }

    public Mono<Void> delete(String id, CurrentUser currentUser) {
        return Mono.fromCallable(() -> findOwned(id, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(story -> Mono.fromRunnable(() -> {
                    if (story.getIllustrationPathLight() != null || story.getIllustrationPathDark() != null) {
                        illustrationService.deleteImage(id);
                    }
                    repository.deleteById(id);
                }).subscribeOn(Schedulers.boundedElastic()))
                .then();
    }

    private Story findOwned(String id, CurrentUser currentUser) {
        var opt = currentUser.isAdmin()
                ? repository.findById(id)
                : repository.findByIdAndUserId(id, currentUser.userId());
        return opt.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Mono<Void> ensureVerified(CurrentUser currentUser) {
        return Mono.fromCallable(() -> users.findById(currentUser.userId())
                        .map(User::isEmailVerified)
                        .orElse(false))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(verified -> verified
                        ? Mono.empty()
                        : Mono.error(new EmailNotVerifiedException()));
    }
}
```

- [ ] **Step 2: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/story/StoryService.java
git commit -m "feat(story): scope queries to currentUser; gate creation on verified email"
```

---

### Task 30: StoryController — resolve current user

**Files:**
- Modify: `backend/src/main/java/com/kazka/story/StoryController.java`

- [ ] **Step 1: Replace controller**

```java
package com.kazka.story;

import com.kazka.auth.CurrentUserResolver;
import com.kazka.story.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/stories")
public class StoryController {

    private final StoryService storyService;
    private final CurrentUserResolver currentUserResolver;

    public StoryController(StoryService storyService, CurrentUserResolver currentUserResolver) {
        this.storyService = storyService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> generate(@Valid @RequestBody GenerationRequest req) {
        return currentUserResolver.requireUser()
                .flatMapMany(cu -> storyService.generate(req, cu))
                .map(event -> ServerSentEvent.builder()
                        .event(event.type())
                        .data(event.data())
                        .build());
    }

    @PostMapping("/{id}/illustrate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> illustrate(@PathVariable String id) {
        return currentUserResolver.requireUser()
                .flatMap(cu -> {
                    storyService.illustrate(id, cu).subscribe();
                    return Mono.empty();
                });
    }

    @GetMapping
    public Mono<PageResponse<StoryDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return currentUserResolver.requireUser().flatMap(cu -> storyService.list(page, size, cu));
    }

    @GetMapping("/{id}")
    public Mono<StoryDto> findById(@PathVariable String id) {
        return currentUserResolver.requireUser().flatMap(cu -> storyService.findById(id, cu));
    }

    @PutMapping("/{id}")
    public Mono<StoryDto> update(@PathVariable String id, @Valid @RequestBody UpdateStoryRequest req) {
        return currentUserResolver.requireUser().flatMap(cu -> storyService.update(id, req, cu));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable String id) {
        return currentUserResolver.requireUser().flatMap(cu -> storyService.delete(id, cu));
    }
}
```

- [ ] **Step 2: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/story/StoryController.java
git commit -m "feat(story): controller resolves current user, passes to service"
```

---

### Task 31: Update existing story tests for userId

**Files:**
- Modify: `backend/src/test/java/com/kazka/story/StoryRepositoryTest.java`
- Modify: `backend/src/test/java/com/kazka/story/StoryControllerTest.java`
- Modify: `backend/src/test/resources/application-test.yml`

- [ ] **Step 1: Update test config to add Redis**

Replace `application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:tc:mysql:8:///kazkar_test
    username: test
    password: test
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  sql:
    init:
      mode: always
  jpa:
    hibernate:
      ddl-auto: none
    defer-datasource-initialization: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  session:
    store-type: redis
  mail:
    host: ${MAIL_HOST:localhost}
    port: ${MAIL_PORT:3025}
    username: ""
    password: ""
    properties:
      mail.smtp.auth: false
      mail.smtp.starttls.enable: false
kazka:
  huggingface:
    api-token: test-token
  auth:
    app-base-url: http://localhost
    mail-from: test@kazka.local
    token-ttl:
      email-verification: 24h
      password-reset: 1h
    admin:
      email: ""
      password: ""
```

- [ ] **Step 2: Add a `userId` to the fixture in StoryRepositoryTest**

Replace the helper at the bottom of `StoryRepositoryTest.java` with:

```java
    private Story story(String title) {
        Story s = new Story();
        s.setId(UUID.randomUUID().toString());
        s.setUserId(seedUser());
        s.setTitle(title);
        s.setTheme("theme");
        s.setCharacters(List.of("Мія", "лисичка"));
        s.setAgeGroup("6-8");
        s.setLength("medium");
        s.setLanguage("uk");
        s.setContent("content");
        s.setIllustrationStatus(IllustrationStatus.PENDING);
        return s;
    }

    @Autowired
    com.kazka.user.UserRepository userRepository;

    private String seedUser() {
        com.kazka.user.User u = new com.kazka.user.User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(u.getId() + "@example.com");
        u.setDisplayName("Test");
        u.setRole(com.kazka.user.UserRole.USER);
        u.setEmailVerified(true);
        userRepository.save(u);
        return u.getId();
    }
```

- [ ] **Step 3: Update StoryControllerTest to expect 401 (no auth)**

Replace existing test methods with:

```java
    @Test
    void getStories_withoutAuth_returns401() {
        webTestClient().get().uri("/api/stories")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getStory_withoutAuth_returns401() {
        webTestClient().get().uri("/api/stories/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isUnauthorized();
    }
```

(The full authenticated coverage moves to `StoryAccessIT` in Task 39.)

- [ ] **Step 4: Run existing tests (Redis container required)**

Run: `cd backend && ./gradlew test --tests "com.kazka.story.StoryRepositoryTest" --tests "com.kazka.story.StoryControllerTest" -q`
Expected: PASS. (If your local docker doesn't have a Redis available, the test Redis container is set up in Task 35 below — defer this step until then.)

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/resources/application-test.yml backend/src/test/java/com/kazka/story/StoryRepositoryTest.java backend/src/test/java/com/kazka/story/StoryControllerTest.java
git commit -m "test(story): add userId fixtures, expect 401 without auth"
```

---

## Phase F — Admin

### Task 32: AdminUserDto, AdminService, AdminController

**Files:**
- Create: `backend/src/main/java/com/kazka/admin/AdminUserDto.java`
- Create: `backend/src/main/java/com/kazka/admin/AdminService.java`
- Create: `backend/src/main/java/com/kazka/admin/AdminController.java`

- [ ] **Step 1: AdminUserDto**

```java
package com.kazka.admin;

import com.kazka.user.UserRole;

import java.time.Instant;

public record AdminUserDto(
        String id,
        String email,
        String displayName,
        UserRole role,
        boolean emailVerified,
        boolean googleLinked,
        Instant createdAt,
        long storyCount
) {}
```

- [ ] **Step 2: AdminService**

```java
package com.kazka.admin;

import com.kazka.story.StoryRepository;
import com.kazka.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository users;
    private final StoryRepository stories;

    public AdminService(UserRepository users, StoryRepository stories) {
        this.users = users;
        this.stories = stories;
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> listUsers() {
        return users.findAllByOrderByCreatedAtDesc().stream()
                .map(u -> new AdminUserDto(
                        u.getId(),
                        u.getEmail(),
                        u.getDisplayName(),
                        u.getRole(),
                        u.isEmailVerified(),
                        u.getGoogleSubject() != null,
                        u.getCreatedAt(),
                        stories.countByUserId(u.getId())))
                .toList();
    }
}
```

- [ ] **Step 3: AdminController**

```java
package com.kazka.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public Mono<List<AdminUserDto>> listUsers() {
        return Mono.fromCallable(adminService::listUsers).subscribeOn(Schedulers.boundedElastic());
    }
}
```

- [ ] **Step 4: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/kazka/admin/
git commit -m "feat(admin): list-users endpoint"
```

---

## Phase G — Bootstrap

### Task 33: UserSeedRunner

**Files:**
- Create: `backend/src/main/java/com/kazka/user/UserSeedRunner.java`

- [ ] **Step 1: Implementation**

```java
package com.kazka.user;

import com.kazka.auth.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class UserSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeedRunner.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties props;

    public UserSeedRunner(UserRepository users, PasswordEncoder passwordEncoder, AuthProperties props) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = props.admin() == null ? null : props.admin().email();
        String password = props.admin() == null ? null : props.admin().password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.info("Admin seeding skipped — ADMIN_EMAIL/ADMIN_PASSWORD not set");
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmail(normalized)) {
            log.info("Admin user {} already present — skipping seed", normalized);
            return;
        }
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(normalized);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setDisplayName("Admin");
        u.setRole(UserRole.ADMIN);
        u.setEmailVerified(true);
        users.save(u);
        log.info("Seeded admin user {}", normalized);
    }
}
```

- [ ] **Step 2: Compile**

Run: `cd backend && ./gradlew compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/user/UserSeedRunner.java
git commit -m "feat(user): seed admin user from env on startup"
```

---

## Phase H — Backend integration tests

### Task 34: AbstractIT base class with Redis + GreenMail + WebTestClient

**Files:**
- Create: `backend/src/test/java/com/kazka/AbstractIT.java`

- [ ] **Step 1: Base class**

```java
package com.kazka;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIT {

    static final RedisContainer REDIS =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(com.icegreen.greenmail.configuration.GreenMailConfiguration
                    .aConfig().withDisabledAuthentication());

    @BeforeAll
    static void startRedis() {
        if (!REDIS.isRunning()) REDIS.start();
    }

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry r) {
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        r.add("spring.mail.host", () -> "localhost");
        r.add("spring.mail.port", () -> ServerSetupTest.SMTP.getPort());
    }

    @LocalServerPort
    int port;

    @Autowired
    org.springframework.context.ApplicationContext ctx;

    protected WebTestClient client() {
        return WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(20))
                .build();
    }
}
```

- [ ] **Step 2: Compile (test classes)**

Run: `cd backend && ./gradlew compileTestJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/kazka/AbstractIT.java
git commit -m "test: AbstractIT base with Redis + GreenMail + WebTestClient"
```

---

### Task 35: AuthControllerIT — signup → me → logout cycle

**Files:**
- Create: `backend/src/test/java/com/kazka/auth/AuthControllerIT.java`

- [ ] **Step 1: Test class**

```java
package com.kazka.auth;

import com.kazka.AbstractIT;
import com.kazka.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerIT extends AbstractIT {

    @Autowired UserRepository users;

    @BeforeEach
    void clean() {
        users.deleteAll();
    }

    @Test
    void should_returnUserAndSessionCookie_when_signup() {
        var result = client().post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "email", "alice@example.com",
                        "password", "password123",
                        "displayName", "Alice"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.user.email").isEqualTo("alice@example.com")
                .returnResult();

        List<String> cookies = result.getResponseHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).anyMatch(c -> c.startsWith("SESSION="));
    }

    @Test
    void should_returnUser_when_callMeWithValidSession() {
        var sessionCookie = signupAndGetSessionCookie("bob@example.com", "Bob");

        client().get().uri("/api/auth/me")
                .header(HttpHeaders.COOKIE, sessionCookie)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.user.email").isEqualTo("bob@example.com");
    }

    @Test
    void should_return401_when_callMeWithoutSession() {
        client().get().uri("/api/auth/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void should_return409_when_signupEmailExists() {
        signupAndGetSessionCookie("dup@example.com", "Dup");

        client().post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "email", "dup@example.com",
                        "password", "password123",
                        "displayName", "Dup2"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.error").isEqualTo("EMAIL_TAKEN");
    }

    private String signupAndGetSessionCookie(String email, String displayName) {
        var result = client().post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "email", email, "password", "password123", "displayName", displayName))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(Void.class);
        ResponseCookie session = result.getResponseCookies().getFirst("SESSION");
        assertThat(session).isNotNull();
        return "SESSION=" + session.getValue();
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd backend && ./gradlew test --tests "com.kazka.auth.AuthControllerIT" -q`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/kazka/auth/AuthControllerIT.java
git commit -m "test(auth): AuthControllerIT — signup, /me, duplicate email"
```

---

### Task 36: EmailVerificationIT

**Files:**
- Create: `backend/src/test/java/com/kazka/auth/EmailVerificationIT.java`

- [ ] **Step 1: Test class**

```java
package com.kazka.auth;

import com.kazka.AbstractIT;
import com.kazka.user.EmailVerificationToken;
import com.kazka.user.EmailVerificationTokenRepository;
import com.kazka.user.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired EmailVerificationTokenRepository tokens;

    @BeforeEach
    void clean() { users.deleteAll(); }

    @Test
    void should_verifyUser_when_clickValidLink() throws Exception {
        signup("eva@example.com");

        String token = waitForVerificationToken();

        client().get().uri("/api/auth/verify-email?token=" + token)
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueMatches("Location", ".*ok=1");

        assertThat(users.findByEmail("eva@example.com")).get()
                .matches(u -> u.isEmailVerified());
    }

    @Test
    void should_redirectWithError_when_tokenInvalid() {
        client().get().uri("/api/auth/verify-email?token=does-not-exist")
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueMatches("Location", ".*error=TOKEN_INVALID");
    }

    @Test
    void should_redirectWithError_when_tokenExpired() {
        signup("late@example.com");
        EmailVerificationToken evt = tokens.findAll().getFirst();
        evt.setExpiresAt(Instant.now().minusSeconds(60));
        tokens.save(evt);

        client().get().uri("/api/auth/verify-email?token=" + evt.getToken())
                .exchange()
                .expectStatus().isSeeOther()
                .expectHeader().valueMatches("Location", ".*error=TOKEN_INVALID");
    }

    private void signup(String email) {
        client().post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", "password123", "displayName", "Test"))
                .exchange()
                .expectStatus().isCreated();
    }

    private String waitForVerificationToken() {
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(greenMail.getReceivedMessages()).hasSizeGreaterThanOrEqualTo(1));
        MimeMessage[] received = greenMail.getReceivedMessages();
        try {
            String body = received[0].getContent().toString();
            Matcher m = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(body);
            assertThat(m.find()).isTrue();
            return m.group(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 2: Add Awaitility test dep if missing**

Run: `cd backend && ./gradlew dependencies --configuration testRuntimeClasspath -q | grep awaitility`
If empty, add to `backend/build.gradle` testImplementation block:

```groovy
    testImplementation 'org.awaitility:awaitility:4.2.2'
```

- [ ] **Step 3: Run tests**

Run: `cd backend && ./gradlew test --tests "com.kazka.auth.EmailVerificationIT" -q`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/kazka/auth/EmailVerificationIT.java backend/build.gradle
git commit -m "test(auth): EmailVerificationIT — happy + invalid + expired"
```

---

### Task 37: PasswordResetIT

**Files:**
- Create: `backend/src/test/java/com/kazka/auth/PasswordResetIT.java`

- [ ] **Step 1: Test class**

```java
package com.kazka.auth;

import com.kazka.AbstractIT;
import com.kazka.user.PasswordResetToken;
import com.kazka.user.PasswordResetTokenRepository;
import com.kazka.user.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        users.deleteAll();
        greenMail.purgeEmailFromAllMailboxes();
    }

    @Test
    void should_return204_when_requestPasswordResetForUnknownEmail() {
        client().post().uri("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "nobody@example.com"))
                .exchange()
                .expectStatus().isNoContent();
        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    @Test
    void should_sendResetEmail_when_requestForKnownEmail() {
        signup("real@example.com");
        greenMail.purgeEmailFromAllMailboxes();

        client().post().uri("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "real@example.com"))
                .exchange()
                .expectStatus().isNoContent();

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(greenMail.getReceivedMessages()).hasSize(1));
    }

    @Test
    void should_updatePasswordHash_when_confirmWithValidToken() throws Exception {
        signup("change@example.com");
        client().post().uri("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "change@example.com"))
                .exchange().expectStatus().isNoContent();

        String token = waitForResetToken();

        client().post().uri("/api/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("token", token, "newPassword", "newpassword456"))
                .exchange()
                .expectStatus().isNoContent();

        var user = users.findByEmail("change@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("newpassword456", user.getPasswordHash())).isTrue();
        PasswordResetToken used = resetTokens.findById(token).orElseThrow();
        assertThat(used.getConsumedAt()).isNotNull();
    }

    @Test
    void should_return400_when_confirmWithUnknownToken() {
        client().post().uri("/api/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("token", "missing", "newPassword", "newpassword456"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo("TOKEN_INVALID");
    }

    private void signup(String email) {
        client().post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", "password123", "displayName", "User"))
                .exchange().expectStatus().isCreated();
    }

    private String waitForResetToken() {
        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(greenMail.getReceivedMessages()).hasSize(1));
        try {
            MimeMessage[] msgs = greenMail.getReceivedMessages();
            String body = msgs[msgs.length - 1].getContent().toString();
            Matcher m = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(body);
            assertThat(m.find()).isTrue();
            return m.group(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 2: Run**

Run: `cd backend && ./gradlew test --tests "com.kazka.auth.PasswordResetIT" -q`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/kazka/auth/PasswordResetIT.java
git commit -m "test(auth): PasswordResetIT — request, confirm, invalid token"
```

---

### Task 38: StoryAccessIT

**Files:**
- Create: `backend/src/test/java/com/kazka/story/StoryAccessIT.java`

- [ ] **Step 1: Test class**

```java
package com.kazka.story;

import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StoryAccessIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired StoryRepository stories;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        stories.deleteAll();
        users.deleteAll();
    }

    @Test
    void should_return404_when_userBReadsUserAStory() {
        User userA = createVerifiedUser("a@example.com", "Apass123!");
        User userB = createVerifiedUser("b@example.com", "Bpass123!");
        Story story = saveStory(userA.getId(), "A's story");
        String sessionB = login("b@example.com", "Bpass123!");

        client().get().uri("/api/stories/" + story.getId())
                .header(HttpHeaders.COOKIE, sessionB)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_return200_when_adminReadsAnyStory() {
        User user = createVerifiedUser("c@example.com", "Cpass123!");
        User admin = createAdmin("admin@example.com", "Adminpass1!");
        Story story = saveStory(user.getId(), "C's story");
        String sessionAdmin = login("admin@example.com", "Adminpass1!");

        client().get().uri("/api/stories/" + story.getId())
                .header(HttpHeaders.COOKIE, sessionAdmin)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void should_listOnlyOwnStories_when_nonAdminListsArchive() {
        User userA = createVerifiedUser("la@example.com", "Apass123!");
        User userB = createVerifiedUser("lb@example.com", "Bpass123!");
        saveStory(userA.getId(), "A1");
        saveStory(userA.getId(), "A2");
        saveStory(userB.getId(), "B1");
        String sessionA = login("la@example.com", "Apass123!");

        client().get().uri("/api/stories")
                .header(HttpHeaders.COOKIE, sessionA)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.total").isEqualTo(2);
    }

    private User createVerifiedUser(String email, String password) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(email);
        u.setDisplayName(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        return users.save(u);
    }

    private User createAdmin(String email, String password) {
        User u = createVerifiedUser(email, password);
        u.setRole(UserRole.ADMIN);
        return users.save(u);
    }

    private Story saveStory(String userId, String title) {
        Story s = new Story();
        s.setId(UUID.randomUUID().toString());
        s.setUserId(userId);
        s.setTitle(title);
        s.setTheme("t");
        s.setCharacters(List.of("hero"));
        s.setAgeGroup("6-8");
        s.setLength("short");
        s.setLanguage("uk");
        s.setContent("body");
        s.setIllustrationStatus(IllustrationStatus.PENDING);
        return stories.save(s);
    }

    private String login(String email, String password) {
        var r = client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", password))
                .exchange()
                .expectStatus().isOk()
                .returnResult(Void.class);
        ResponseCookie c = r.getResponseCookies().getFirst("SESSION");
        assertThat(c).isNotNull();
        return "SESSION=" + c.getValue();
    }
}
```

- [ ] **Step 2: Run**

Run: `cd backend && ./gradlew test --tests "com.kazka.story.StoryAccessIT" -q`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/kazka/story/StoryAccessIT.java
git commit -m "test(story): user-scoped access + admin bypass"
```

---

### Task 39: AdminControllerIT + SeedAdminIT

**Files:**
- Create: `backend/src/test/java/com/kazka/admin/AdminControllerIT.java`
- Create: `backend/src/test/java/com/kazka/user/SeedAdminIT.java`

- [ ] **Step 1: AdminControllerIT**

```java
package com.kazka.admin;

import com.kazka.AbstractIT;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminControllerIT extends AbstractIT {

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() { users.deleteAll(); }

    @Test
    void should_return403_when_nonAdminListsUsers() {
        seed("u@example.com", "Userpass1!", UserRole.USER);
        String session = login("u@example.com", "Userpass1!");

        client().get().uri("/api/admin/users")
                .header(HttpHeaders.COOKIE, session)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void should_returnUsersWithoutPasswordHash_when_adminListsUsers() {
        seed("a@example.com", "Adminpass1!", UserRole.ADMIN);
        seed("u@example.com", "Userpass1!", UserRole.USER);
        String session = login("a@example.com", "Adminpass1!");

        client().get().uri("/api/admin/users")
                .header(HttpHeaders.COOKIE, session)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$[0].passwordHash").doesNotExist()
                .jsonPath("$[0].googleSubject").doesNotExist();
    }

    private void seed(String email, String password, UserRole role) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(email);
        u.setDisplayName(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(role);
        u.setEmailVerified(true);
        users.save(u);
    }

    private String login(String email, String password) {
        var r = client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", email, "password", password))
                .exchange()
                .expectStatus().isOk()
                .returnResult(Void.class);
        ResponseCookie c = r.getResponseCookies().getFirst("SESSION");
        assertThat(c).isNotNull();
        return "SESSION=" + c.getValue();
    }
}
```

- [ ] **Step 2: SeedAdminIT**

```java
package com.kazka.user;

import com.kazka.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "kazka.auth.admin.email=seed-admin@example.com",
        "kazka.auth.admin.password=Seedpass1!"
})
class SeedAdminIT extends AbstractIT {

    @Autowired UserRepository users;

    @Test
    void should_createAdmin_when_envEmailAndPasswordAreSet() {
        var seeded = users.findByEmail("seed-admin@example.com").orElseThrow();
        assertThat(seeded.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(seeded.isEmailVerified()).isTrue();
    }
}
```

- [ ] **Step 3: Run**

Run: `cd backend && ./gradlew test --tests "com.kazka.admin.AdminControllerIT" --tests "com.kazka.user.SeedAdminIT" -q`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/kazka/admin/AdminControllerIT.java backend/src/test/java/com/kazka/user/SeedAdminIT.java
git commit -m "test(admin,seed): role-gated listing + admin auto-seed"
```

---

### Task 39b: OAuth2SuccessHandler unit test

**Files:**
- Create: `backend/src/test/java/com/kazka/auth/OAuth2SuccessHandlerTest.java`

- [ ] **Step 1: Test class**

```java
package com.kazka.auth;

import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuth2SuccessHandlerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final AuthProperties props = new AuthProperties(
            "http://localhost", "from@example.com",
            new AuthProperties.TokenTtl(java.time.Duration.ofHours(24), java.time.Duration.ofHours(1)),
            new AuthProperties.Admin("", ""));
    private final OAuth2SuccessHandler handler = new OAuth2SuccessHandler(users, props);

    @Test
    void should_returnExistingUser_when_googleSubjectFound() {
        User existing = userOf("a@example.com", "sub-1", null);
        when(users.findByGoogleSubject("sub-1")).thenReturn(Optional.of(existing));

        User resolved = handler.resolveOrCreateUser("sub-1", "a@example.com", "Alice");

        assertThat(resolved).isSameAs(existing);
    }

    @Test
    void should_linkAndVerify_when_emailFoundWithoutGoogleSubject() {
        User existing = userOf("b@example.com", null, null);
        existing.setEmailVerified(false);
        when(users.findByGoogleSubject("sub-2")).thenReturn(Optional.empty());
        when(users.findByEmail("b@example.com")).thenReturn(Optional.of(existing));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User resolved = handler.resolveOrCreateUser("sub-2", "b@example.com", "Bob");

        assertThat(resolved.getGoogleSubject()).isEqualTo("sub-2");
        assertThat(resolved.isEmailVerified()).isTrue();
    }

    @Test
    void should_createUser_when_neitherSubjectNorEmailFound() {
        when(users.findByGoogleSubject("sub-3")).thenReturn(Optional.empty());
        when(users.findByEmail("c@example.com")).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User resolved = handler.resolveOrCreateUser("sub-3", "c@example.com", "Carol");

        assertThat(resolved.getEmail()).isEqualTo("c@example.com");
        assertThat(resolved.getGoogleSubject()).isEqualTo("sub-3");
        assertThat(resolved.isEmailVerified()).isTrue();
        assertThat(resolved.getRole()).isEqualTo(UserRole.USER);
        assertThat(resolved.getDisplayName()).isEqualTo("Carol");
    }

    private User userOf(String email, String sub, String hash) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail(email);
        u.setGoogleSubject(sub);
        u.setPasswordHash(hash);
        u.setDisplayName(email);
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        return u;
    }
}
```

- [ ] **Step 2: Run**

Run: `cd backend && ./gradlew test --tests "com.kazka.auth.OAuth2SuccessHandlerTest" -q`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/kazka/auth/OAuth2SuccessHandlerTest.java
git commit -m "test(auth): OAuth2SuccessHandler unit tests for three branches"
```

---

### Task 40: Run the full backend test suite

- [ ] **Step 1: Full run**

Run: `cd backend && ./gradlew test -q`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 2: If failures, fix in-place**

For each failure: read the assertion, check the production code, fix; re-run only the failing test.

- [ ] **Step 3: Commit any fixes**

```bash
git add backend/
git commit -m "fix(auth): address full-suite issues"
```

(Skip this if no fixes needed.)

---

## Phase I — Frontend plumbing

### Task 41: csrf helper

**Files:**
- Create: `frontend/src/lib/csrf.ts`

- [ ] **Step 1: Helper**

```ts
export function readCsrfCookie(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)
  return match ? decodeURIComponent(match[1]) : null
}

export function withCsrf(init: RequestInit = {}): RequestInit {
  const method = (init.method ?? 'GET').toUpperCase()
  const safe = method === 'GET' || method === 'HEAD' || method === 'OPTIONS'
  const headers = new Headers(init.headers)
  if (!safe) {
    const token = readCsrfCookie()
    if (token) headers.set('X-XSRF-TOKEN', token)
  }
  return { ...init, headers, credentials: 'include' }
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/lib/csrf.ts
git commit -m "feat(frontend): csrf helper for cookie + header pairing"
```

---

### Task 42: types.ts — User and ApiError

**Files:**
- Modify: `frontend/src/lib/types.ts`

- [ ] **Step 1: Append types**

Append to `types.ts`:

```ts
export type UserRole = 'USER' | 'ADMIN'

export interface User {
  id: string
  email: string
  displayName: string
  role: UserRole
  emailVerified: boolean
  googleLinked: boolean
}

export type AuthErrorCode =
  | 'INVALID_CREDENTIALS'
  | 'EMAIL_TAKEN'
  | 'EMAIL_NOT_VERIFIED'
  | 'TOKEN_INVALID'
  | 'MAIL_SEND_FAILED'
  | 'VALIDATION'
  | 'UNAUTHENTICATED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'ERROR'

export interface ApiErrorBody {
  error: AuthErrorCode | string
  message?: string
  fields?: Record<string, string>
}

export class ApiError extends Error {
  constructor(public status: number, public body: ApiErrorBody) {
    super(body.error)
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/lib/types.ts
git commit -m "feat(frontend): User type + ApiError shape"
```

---

### Task 43: apiClient — credentials, CSRF, auth + admin methods

**Files:**
- Modify: `frontend/src/lib/apiClient.ts`

- [ ] **Step 1: Replace file**

```ts
import { withCsrf } from './csrf'
import { ApiError } from './types'
import type {
  Story, PageResponse, UpdateStoryRequest, User, ApiErrorBody,
} from './types'

const STORIES = '/api/stories'
const AUTH = '/api/auth'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, withCsrf({
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    ...init,
  }))
  if (!res.ok) {
    let body: ApiErrorBody
    try { body = await res.json() } catch { body = { error: 'ERROR' } }
    throw new ApiError(res.status, body)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export const api = {
  listStories(page = 0, size = 20): Promise<PageResponse<Story>> {
    return request(`${STORIES}?page=${page}&size=${size}`)
  },
  getStory(id: string): Promise<Story> {
    return request(`${STORIES}/${id}`)
  },
  updateStory(id: string, body: UpdateStoryRequest): Promise<Story> {
    return request(`${STORIES}/${id}`, { method: 'PUT', body: JSON.stringify(body) })
  },
  deleteStory(id: string): Promise<void> {
    return request(`${STORIES}/${id}`, { method: 'DELETE' })
  },
  illustrate(id: string): Promise<void> {
    return request(`${STORIES}/${id}/illustrate`, { method: 'POST' })
  },
}

interface AuthEnvelope { user: User }

export const auth = {
  signup(email: string, password: string, displayName: string): Promise<AuthEnvelope> {
    return request(`${AUTH}/signup`, { method: 'POST', body: JSON.stringify({ email, password, displayName }) })
  },
  login(email: string, password: string): Promise<AuthEnvelope> {
    return request(`${AUTH}/login`, { method: 'POST', body: JSON.stringify({ email, password }) })
  },
  logout(): Promise<void> {
    return request(`${AUTH}/logout`, { method: 'POST' })
  },
  me(): Promise<AuthEnvelope | null> {
    return request<AuthEnvelope>(`${AUTH}/me`).catch(err => {
      if (err instanceof ApiError && err.status === 401) return null
      throw err
    })
  },
  resendVerification(): Promise<void> {
    return request(`${AUTH}/verify-email/resend`, { method: 'POST' })
  },
  passwordResetRequest(email: string): Promise<void> {
    return request(`${AUTH}/password-reset/request`, { method: 'POST', body: JSON.stringify({ email }) })
  },
  passwordResetConfirm(token: string, newPassword: string): Promise<void> {
    return request(`${AUTH}/password-reset/confirm`, { method: 'POST', body: JSON.stringify({ token, newPassword }) })
  },
}

export interface AdminUser {
  id: string
  email: string
  displayName: string
  role: 'USER' | 'ADMIN'
  emailVerified: boolean
  googleLinked: boolean
  createdAt: string
  storyCount: number
}

export const admin = {
  listUsers(): Promise<AdminUser[]> {
    return request(`/api/admin/users`)
  },
}
```

- [ ] **Step 2: Verify types**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/lib/apiClient.ts
git commit -m "feat(frontend): credentials+CSRF; auth+admin api methods"
```

---

### Task 44: sseClient — credentials + CSRF on POST

**Files:**
- Modify: `frontend/src/lib/sseClient.ts`

- [ ] **Step 1: Patch the `fetch` call**

Replace the `const res = await fetch(...)` block with:

```ts
import { withCsrf } from './csrf'

// ...inside streamStory:
  const res = await fetch('/api/stories/generate', withCsrf({
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify(req),
    signal,
  }))
```

(Add the `import { withCsrf } from './csrf'` at the top of the file.)

- [ ] **Step 2: Verify types**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/lib/sseClient.ts
git commit -m "feat(frontend): SSE POST sends credentials + CSRF token"
```

---

### Task 45: AuthContext

**Files:**
- Create: `frontend/src/lib/AuthContext.tsx`

- [ ] **Step 1: Implementation**

```tsx
import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { auth as authApi } from './apiClient'
import type { User } from './types'

interface AuthCtx {
  user: User | null
  loading: boolean
  signIn: (email: string, password: string) => Promise<void>
  signUp: (email: string, password: string, displayName: string) => Promise<void>
  signOut: () => Promise<void>
  requestPasswordReset: (email: string) => Promise<void>
  confirmPasswordReset: (token: string, newPassword: string) => Promise<void>
  resendVerification: () => Promise<void>
  refresh: () => Promise<void>
}

const AuthCtx = createContext<AuthCtx | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    const env = await authApi.me()
    setUser(env?.user ?? null)
  }, [])

  useEffect(() => {
    refresh().finally(() => setLoading(false))
  }, [refresh])

  const signIn = useCallback(async (email: string, password: string) => {
    const env = await authApi.login(email, password)
    setUser(env.user)
  }, [])

  const signUp = useCallback(async (email: string, password: string, displayName: string) => {
    const env = await authApi.signup(email, password, displayName)
    setUser(env.user)
  }, [])

  const signOut = useCallback(async () => {
    await authApi.logout()
    setUser(null)
  }, [])

  const value: AuthCtx = {
    user, loading, signIn, signUp, signOut, refresh,
    requestPasswordReset: authApi.passwordResetRequest,
    confirmPasswordReset: authApi.passwordResetConfirm,
    resendVerification: authApi.resendVerification,
  }

  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>
}

export function useAuth(): AuthCtx {
  const ctx = useContext(AuthCtx)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
```

- [ ] **Step 2: Verify types**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/lib/AuthContext.tsx
git commit -m "feat(frontend): AuthContext with /me hydration"
```

---

### Task 46: AuthModalContext

**Files:**
- Create: `frontend/src/lib/AuthModalContext.tsx`

- [ ] **Step 1: Implementation**

```tsx
import { createContext, useCallback, useContext, useState } from 'react'
import type { ReactNode } from 'react'

export type AuthTab = 'signIn' | 'signUp' | 'forgot'

interface Ctx {
  open: boolean
  tab: AuthTab
  openAuth: (tab?: AuthTab) => void
  closeAuth: () => void
  setTab: (tab: AuthTab) => void
}

const AuthModalCtx = createContext<Ctx | null>(null)

export function AuthModalProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false)
  const [tab, setTab] = useState<AuthTab>('signIn')

  const openAuth = useCallback((next: AuthTab = 'signIn') => {
    setTab(next)
    setOpen(true)
  }, [])

  const closeAuth = useCallback(() => setOpen(false), [])

  return (
    <AuthModalCtx.Provider value={{ open, tab, openAuth, closeAuth, setTab }}>
      {children}
    </AuthModalCtx.Provider>
  )
}

export function useAuthModal(): Ctx {
  const ctx = useContext(AuthModalCtx)
  if (!ctx) throw new Error('useAuthModal must be used within AuthModalProvider')
  return ctx
}
```

- [ ] **Step 2: Verify types**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/lib/AuthModalContext.tsx
git commit -m "feat(frontend): AuthModalContext with tab state"
```

---

### Task 47: Locales — auth strings

**Files:**
- Modify: `frontend/src/locales/uk.ts`
- Modify: `frontend/src/locales/en.ts`

- [ ] **Step 1: Append `auth` block to `uk.ts`**

Inside the `uk` object, before the closing brace, add:

```ts
  auth: {
    tabs: { signIn: 'Увійти', signUp: 'Реєстрація', forgot: 'Забули пароль?' },
    fields: {
      email: 'Електронна пошта',
      displayName: 'Ваше ім\'я',
      password: 'Пароль',
      confirmPassword: 'Повторіть пароль',
      newPassword: 'Новий пароль',
    },
    actions: {
      signIn: 'Увійти',
      signUp: 'Створити акаунт',
      google: 'Продовжити з Google',
      sendResetLink: 'Надіслати посилання',
      submitReset: 'Оновити пароль',
      resend: 'Надіслати посилання повторно',
      signOut: 'Вийти',
      myArchive: 'Мій архів',
      adminUsers: 'Адмін → Користувачі',
    },
    errors: {
      INVALID_CREDENTIALS: 'Неправильна пошта або пароль.',
      EMAIL_TAKEN: 'Цей акаунт уже існує. Спробуйте увійти.',
      EMAIL_NOT_VERIFIED: 'Спочатку підтвердьте свою пошту.',
      TOKEN_INVALID: 'Посилання недійсне або вже використане.',
      MAIL_SEND_FAILED: 'Не вдалося надіслати лист. Спробуйте пізніше.',
      VALIDATION: 'Перевірте, будь ласка, заповнення полів.',
      passwordMismatch: 'Паролі не співпадають.',
      passwordTooShort: 'Мінімум 8 символів.',
      ERROR: 'Щось пішло не так.',
    },
    messages: {
      checkEmail: 'Перевірте пошту — ми надіслали лист для підтвердження.',
      resetSent: 'Якщо такий акаунт існує, ми надіслали лист.',
      passwordUpdated: 'Пароль оновлено. Увійдіть із новим паролем.',
      verifySuccess: 'Пошту підтверджено! Тепер можете створювати казки.',
      verifyError: 'Посилання недійсне або застаріло. Запросіть нове.',
      verifyPanelTitle: 'Підтвердьте свою пошту',
      verifyPanelBody: (email: string) =>
        `Ми надіслали посилання на ${email}. Підтвердьте пошту, щоб почати створювати казки.`,
    },
  },
```

- [ ] **Step 2: Append the parallel `auth` block to `en.ts`**

```ts
  auth: {
    tabs: { signIn: 'Sign in', signUp: 'Sign up', forgot: 'Forgot password?' },
    fields: {
      email: 'Email',
      displayName: 'Your name',
      password: 'Password',
      confirmPassword: 'Confirm password',
      newPassword: 'New password',
    },
    actions: {
      signIn: 'Sign in',
      signUp: 'Create account',
      google: 'Continue with Google',
      sendResetLink: 'Send reset link',
      submitReset: 'Update password',
      resend: 'Resend link',
      signOut: 'Sign out',
      myArchive: 'My archive',
      adminUsers: 'Admin → Users',
    },
    errors: {
      INVALID_CREDENTIALS: 'Invalid email or password.',
      EMAIL_TAKEN: 'That email is already registered. Try signing in.',
      EMAIL_NOT_VERIFIED: 'Please verify your email first.',
      TOKEN_INVALID: 'The link is invalid or has already been used.',
      MAIL_SEND_FAILED: 'Could not send the email. Please try again later.',
      VALIDATION: 'Please check the form for errors.',
      passwordMismatch: 'Passwords do not match.',
      passwordTooShort: 'At least 8 characters.',
      ERROR: 'Something went wrong.',
    },
    messages: {
      checkEmail: 'Check your email — we sent a confirmation link.',
      resetSent: 'If that email exists, we sent a reset link.',
      passwordUpdated: 'Password updated. Sign in with your new password.',
      verifySuccess: 'Email verified! You can now create stories.',
      verifyError: 'The link is invalid or expired. Request a new one.',
      verifyPanelTitle: 'Verify your email',
      verifyPanelBody: (email: string) =>
        `We sent a confirmation link to ${email}. Verify your email to start creating stories.`,
    },
  },
```

- [ ] **Step 3: Verify types**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/locales/uk.ts frontend/src/locales/en.ts
git commit -m "i18n(frontend): auth strings (uk + en)"
```

---

## Phase J — Frontend UI

### Task 48: AuthModal CSS

**Files:**
- Create: `frontend/src/components/auth/AuthModal.module.css`

- [ ] **Step 1: Styles**

```css
.tabs {
  display: flex;
  gap: 8px;
  padding: 0 24px 8px;
}
.tab {
  flex: 1;
  border: none;
  background: transparent;
  font-family: inherit;
  font-size: 0.95rem;
  padding: 10px 12px;
  color: var(--color-text-muted);
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.tab:hover { color: var(--color-text); }
.tabActive {
  background: var(--color-magic, #C4B5FD);
  color: #fff;
}
.form { display: flex; flex-direction: column; gap: 14px; padding: 6px 24px 4px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.label { font-size: 0.85rem; color: var(--color-text-muted); font-weight: 500; }
.input {
  border: 1px solid var(--color-surface-deep);
  background: var(--color-surface-elev, var(--color-surface));
  color: var(--color-text);
  border-radius: 10px;
  padding: 11px 13px;
  font-size: 0.95rem;
  font-family: inherit;
}
.input:focus { outline: 2px solid var(--color-magic, #C4B5FD); outline-offset: 1px; }
.fieldError { color: #B91C1C; font-size: 0.8rem; }
.submit {
  background: linear-gradient(90deg, var(--color-ember, #D97706), var(--color-magic, #7C3AED));
  color: #fff;
  border: none;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 1rem;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: transform 0.15s;
}
.submit:disabled { opacity: 0.6; cursor: progress; }
.submit:hover:not(:disabled) { transform: translateY(-1px); }
.googleBtn {
  background: var(--color-surface-deep);
  color: var(--color-text);
  border: 1px solid var(--color-surface-deep);
  border-radius: 12px;
  padding: 11px 16px;
  font-size: 0.95rem;
  text-decoration: none;
  text-align: center;
  display: block;
  margin: 4px 24px 24px;
}
.googleBtn:hover { background: var(--color-surface); }
.divider {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--color-text-faint);
  font-size: 0.8rem;
  margin: 6px 24px;
}
.divider::before, .divider::after { content: ''; flex: 1; height: 1px; background: var(--color-surface-deep); }
.linkBtn {
  background: none;
  border: none;
  color: var(--color-magic, #7C3AED);
  cursor: pointer;
  font-size: 0.85rem;
  padding: 0;
  text-decoration: underline;
  font-family: inherit;
  align-self: flex-start;
  margin: -6px 0 0;
}
.banner {
  background: #FEE2E2;
  color: #991B1B;
  padding: 10px 14px;
  border-radius: 8px;
  margin: 0 24px 6px;
  font-size: 0.88rem;
}
.bannerInfo {
  background: #DCFCE7;
  color: #14532D;
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/auth/AuthModal.module.css
git commit -m "style(auth): AuthModal css"
```

---

### Task 49: GoogleButton

**Files:**
- Create: `frontend/src/components/auth/GoogleButton.tsx`

- [ ] **Step 1: Component**

```tsx
import { useLocale } from '../../lib/LocaleContext'
import styles from './AuthModal.module.css'

export function GoogleButton() {
  const { t } = useLocale()
  return (
    <a className={styles.googleBtn} href="/oauth2/authorization/google">
      {t.auth.actions.google}
    </a>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/auth/GoogleButton.tsx
git commit -m "feat(auth): GoogleButton anchor"
```

---

### Task 50: SignInForm

**Files:**
- Create: `frontend/src/components/auth/SignInForm.tsx`

- [ ] **Step 1: Component**

```tsx
import { useState } from 'react'
import { useAuth } from '../../lib/AuthContext'
import { useLocale } from '../../lib/LocaleContext'
import { ApiError } from '../../lib/types'
import { useAuthModal } from '../../lib/AuthModalContext'
import styles from './AuthModal.module.css'

export function SignInForm({ onSuccess }: { onSuccess: () => void }) {
  const { signIn } = useAuth()
  const { setTab } = useAuthModal()
  const { t } = useLocale()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handle(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await signIn(email.trim(), password)
      onSuccess()
    } catch (err) {
      const code = err instanceof ApiError ? err.body.error : 'ERROR'
      setError(t.auth.errors[code as keyof typeof t.auth.errors] ?? t.auth.errors.ERROR)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className={styles.form} onSubmit={handle}>
      {error && <div className={styles.banner}>{error}</div>}
      <label className={styles.field}>
        <span className={styles.label}>{t.auth.fields.email}</span>
        <input className={styles.input} type="email" required value={email}
               onChange={e => setEmail(e.target.value)} disabled={submitting} />
      </label>
      <label className={styles.field}>
        <span className={styles.label}>{t.auth.fields.password}</span>
        <input className={styles.input} type="password" required value={password}
               onChange={e => setPassword(e.target.value)} disabled={submitting} />
      </label>
      <button type="button" className={styles.linkBtn} onClick={() => setTab('forgot')}>
        {t.auth.tabs.forgot}
      </button>
      <button type="submit" className={styles.submit} disabled={submitting}>
        {submitting ? '…' : t.auth.actions.signIn}
      </button>
    </form>
  )
}
```

- [ ] **Step 2: Verify types**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/auth/SignInForm.tsx
git commit -m "feat(auth): SignInForm"
```

---

### Task 51: SignUpForm

**Files:**
- Create: `frontend/src/components/auth/SignUpForm.tsx`

- [ ] **Step 1: Component**

```tsx
import { useState } from 'react'
import { useAuth } from '../../lib/AuthContext'
import { useLocale } from '../../lib/LocaleContext'
import { ApiError } from '../../lib/types'
import styles from './AuthModal.module.css'

export function SignUpForm({ onSuccess }: { onSuccess: () => void }) {
  const { signUp } = useAuth()
  const { t } = useLocale()
  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handle(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    if (password !== confirm) { setError(t.auth.errors.passwordMismatch); return }
    if (password.length < 8) { setError(t.auth.errors.passwordTooShort); return }
    setSubmitting(true)
    try {
      await signUp(email.trim(), password, displayName.trim())
      onSuccess()
    } catch (err) {
      const code = err instanceof ApiError ? err.body.error : 'ERROR'
      setError(t.auth.errors[code as keyof typeof t.auth.errors] ?? t.auth.errors.ERROR)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className={styles.form} onSubmit={handle}>
      {error && <div className={styles.banner}>{error}</div>}
      <label className={styles.field}>
        <span className={styles.label}>{t.auth.fields.email}</span>
        <input className={styles.input} type="email" required value={email}
               onChange={e => setEmail(e.target.value)} disabled={submitting} />
      </label>
      <label className={styles.field}>
        <span className={styles.label}>{t.auth.fields.displayName}</span>
        <input className={styles.input} type="text" required maxLength={100} value={displayName}
               onChange={e => setDisplayName(e.target.value)} disabled={submitting} />
      </label>
      <label className={styles.field}>
        <span className={styles.label}>{t.auth.fields.password}</span>
        <input className={styles.input} type="password" required minLength={8} value={password}
               onChange={e => setPassword(e.target.value)} disabled={submitting} />
      </label>
      <label className={styles.field}>
        <span className={styles.label}>{t.auth.fields.confirmPassword}</span>
        <input className={styles.input} type="password" required minLength={8} value={confirm}
               onChange={e => setConfirm(e.target.value)} disabled={submitting} />
      </label>
      <button type="submit" className={styles.submit} disabled={submitting}>
        {submitting ? '…' : t.auth.actions.signUp}
      </button>
    </form>
  )
}
```

- [ ] **Step 2: Verify types**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/auth/SignUpForm.tsx
git commit -m "feat(auth): SignUpForm with client-side password validation"
```

---

### Task 52: ForgotPasswordForm

**Files:**
- Create: `frontend/src/components/auth/ForgotPasswordForm.tsx`

- [ ] **Step 1: Component**

```tsx
import { useState } from 'react'
import { useAuth } from '../../lib/AuthContext'
import { useLocale } from '../../lib/LocaleContext'
import styles from './AuthModal.module.css'

export function ForgotPasswordForm() {
  const { requestPasswordReset } = useAuth()
  const { t } = useLocale()
  const [email, setEmail] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)

  async function handle(e: React.FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    try {
      await requestPasswordReset(email.trim())
      setDone(true)
    } finally {
      setSubmitting(false)
    }
  }

  if (done) {
    return <div className={`${styles.banner} ${styles.bannerInfo}`}>{t.auth.messages.resetSent}</div>
  }

  return (
    <form className={styles.form} onSubmit={handle}>
      <label className={styles.field}>
        <span className={styles.label}>{t.auth.fields.email}</span>
        <input className={styles.input} type="email" required value={email}
               onChange={e => setEmail(e.target.value)} disabled={submitting} />
      </label>
      <button type="submit" className={styles.submit} disabled={submitting}>
        {submitting ? '…' : t.auth.actions.sendResetLink}
      </button>
    </form>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/auth/ForgotPasswordForm.tsx
git commit -m "feat(auth): ForgotPasswordForm"
```

---

### Task 53: AuthModal

**Files:**
- Create: `frontend/src/components/auth/AuthModal.tsx`

- [ ] **Step 1: Component**

```tsx
import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import { useAuthModal } from '../../lib/AuthModalContext'
import { useLocale } from '../../lib/LocaleContext'
import { SignInForm } from './SignInForm'
import { SignUpForm } from './SignUpForm'
import { ForgotPasswordForm } from './ForgotPasswordForm'
import { GoogleButton } from './GoogleButton'
import storyStyles from '../modal/StoryModal.module.css'
import styles from './AuthModal.module.css'

export function AuthModal() {
  const { open, tab, closeAuth, setTab } = useAuthModal()
  const { t } = useLocale()

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') closeAuth() }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, closeAuth])

  useEffect(() => {
    document.body.style.overflow = open ? 'hidden' : ''
    return () => { document.body.style.overflow = '' }
  }, [open])

  if (!open) return null

  return createPortal(
    <div className={storyStyles.backdrop} onClick={closeAuth} role="dialog" aria-modal="true">
      <div className={storyStyles.panel} onClick={e => e.stopPropagation()}>
        <div className={storyStyles.topBorder} />
        <div className={storyStyles.header}>
          <button className={storyStyles.closeBtn} onClick={closeAuth} aria-label="Close">✕</button>
        </div>
        <div className={styles.tabs}>
          <button className={`${styles.tab} ${tab === 'signIn' ? styles.tabActive : ''}`} onClick={() => setTab('signIn')}>
            {t.auth.tabs.signIn}
          </button>
          <button className={`${styles.tab} ${tab === 'signUp' ? styles.tabActive : ''}`} onClick={() => setTab('signUp')}>
            {t.auth.tabs.signUp}
          </button>
        </div>
        <div className={storyStyles.body}>
          {tab === 'signIn' && <SignInForm onSuccess={closeAuth} />}
          {tab === 'signUp' && <SignUpForm onSuccess={closeAuth} />}
          {tab === 'forgot' && <ForgotPasswordForm />}
        </div>
        {tab !== 'forgot' && (
          <>
            <div className={styles.divider}>{tab === 'signIn' ? 'or' : 'or'}</div>
            <GoogleButton />
          </>
        )}
      </div>
    </div>,
    document.body,
  )
}
```

- [ ] **Step 2: Verify types**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/auth/AuthModal.tsx
git commit -m "feat(auth): AuthModal with tabbed sign-in/sign-up/forgot"
```

---

### Task 54: RequireAuth + RequireAdmin

**Files:**
- Create: `frontend/src/components/auth/RequireAuth.tsx`
- Create: `frontend/src/components/auth/RequireAdmin.tsx`

- [ ] **Step 1: RequireAuth**

```tsx
import { useEffect } from 'react'
import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../../lib/AuthContext'
import { useAuthModal } from '../../lib/AuthModalContext'

export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  const { openAuth } = useAuthModal()

  useEffect(() => {
    if (!loading && !user) openAuth('signIn')
  }, [loading, user, openAuth])

  if (loading) return <p style={{ padding: 32, textAlign: 'center' }}>...</p>
  if (!user) return <Navigate to="/" replace />
  return <>{children}</>
}
```

- [ ] **Step 2: RequireAdmin**

```tsx
import type { ReactNode } from 'react'
import { useAuth } from '../../lib/AuthContext'

export function RequireAdmin({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()

  if (loading) return <p style={{ padding: 32, textAlign: 'center' }}>...</p>
  if (!user || user.role !== 'ADMIN') {
    return <p style={{ padding: 32, textAlign: 'center' }}>404</p>
  }
  return <>{children}</>
}
```

- [ ] **Step 3: Verify types**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/auth/RequireAuth.tsx frontend/src/components/auth/RequireAdmin.tsx
git commit -m "feat(auth): RequireAuth + RequireAdmin route wrappers"
```

---

### Task 55: Nav.tsx — auth-aware

**Files:**
- Modify: `frontend/src/components/chrome/Nav.tsx`
- Modify: `frontend/src/components/chrome/Nav.module.css`

- [ ] **Step 1: Replace Nav.tsx**

```tsx
import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useLocale } from '../../lib/LocaleContext'
import { useTheme } from '../../lib/ThemeContext'
import { useStoryModal } from '../../lib/StoryModalContext'
import { useAuth } from '../../lib/AuthContext'
import { useAuthModal } from '../../lib/AuthModalContext'
import styles from './Nav.module.css'

export function Nav() {
  const { toggleLang, t } = useLocale()
  const { theme, toggleTheme } = useTheme()
  const { openModal } = useStoryModal()
  const { user, signOut } = useAuth()
  const { openAuth } = useAuthModal()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [scrolled, setScrolled] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handler = () => setScrolled(window.scrollY > 80)
    window.addEventListener('scroll', handler, { passive: true })
    handler()
    return () => window.removeEventListener('scroll', handler)
  }, [])

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false)
    }
    document.addEventListener('click', onClick)
    return () => document.removeEventListener('click', onClick)
  }, [])

  const tryClick = (e: React.MouseEvent) => {
    e.preventDefault()
    if (!user) openAuth('signIn'); else openModal()
  }

  return (
    <nav className={`${styles.nav} ${scrolled ? styles.scrolled : ''}`}>
      <Link to="/" className={styles.logo}>
        <svg viewBox="0 0 28 28" fill="none" className={styles.logoIcon} aria-hidden="true">
          <path d="M6 4C6 4 8 6 8 14C8 22 6 24 6 24" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
          <path d="M6 4C10 4 20 4 22 6C24 8 24 10 22 12C20 14 14 14 14 14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          <path d="M6 14C10 14 18 14 20 16C22 18 22 20 20 22C18 24 10 24 6 24" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
          <circle cx="21" cy="5" r="1.5" fill="#C4B5FD" opacity="0.7"/>
          <circle cx="24" cy="9" r="1" fill="#C4B5FD" opacity="0.5"/>
        </svg>
        <span>Казкар</span>
      </Link>

      <ul className={styles.links}>
        <li><a href="/#how" className={styles.link}>{t.nav.howItWorks}</a></li>
        <li><a href="/#features" className={styles.link}>{t.nav.features}</a></li>
        {user && (
          <li>
            <Link to="/stories"
                  className={pathname.startsWith('/stories') ? `${styles.link} ${styles.active}` : styles.link}>
              {t.nav.archive}
            </Link>
          </li>
        )}
        <li>
          <button onClick={toggleTheme} className={styles.themeToggle} aria-label={t.nav.toggleTheme}>
            {theme === 'light' ? t.nav.themeLight : t.nav.themeDark}
          </button>
        </li>
        <li>
          <button onClick={toggleLang} className={styles.langBtn} aria-label="Toggle language">
            {t.nav.toggleLang}
          </button>
        </li>
        {!user && (
          <>
            <li>
              <button className={styles.link} onClick={() => openAuth('signIn')}>{t.auth.tabs.signIn}</button>
            </li>
            <li>
              <button className={styles.ctaBtn} onClick={() => openAuth('signUp')}>{t.auth.tabs.signUp}</button>
            </li>
          </>
        )}
        {user && (
          <>
            <li>
              <a href="#" className={styles.ctaBtn} onClick={tryClick}>{t.nav.tryCta}</a>
            </li>
            <li className={styles.userWrap} ref={menuRef}>
              <button className={styles.userBtn} onClick={() => setMenuOpen(o => !o)}>{user.displayName} ▾</button>
              {menuOpen && (
                <div className={styles.userMenu}>
                  <button onClick={() => { setMenuOpen(false); navigate('/stories') }}>{t.auth.actions.myArchive}</button>
                  {user.role === 'ADMIN' && (
                    <button onClick={() => { setMenuOpen(false); navigate('/admin/users') }}>{t.auth.actions.adminUsers}</button>
                  )}
                  <button onClick={async () => { setMenuOpen(false); await signOut(); navigate('/') }}>{t.auth.actions.signOut}</button>
                </div>
              )}
            </li>
          </>
        )}
      </ul>
    </nav>
  )
}
```

- [ ] **Step 2: Append dropdown styles to `Nav.module.css`**

```css
.userWrap { position: relative; }
.userBtn {
  background: transparent;
  border: 1px solid var(--color-surface-deep);
  border-radius: 999px;
  padding: 6px 14px;
  font-family: inherit;
  font-size: 0.9rem;
  color: var(--color-text);
  cursor: pointer;
}
.userMenu {
  position: absolute;
  right: 0;
  top: 100%;
  margin-top: 8px;
  background: var(--color-surface);
  border: 1px solid var(--color-surface-deep);
  border-radius: 12px;
  min-width: 180px;
  display: flex;
  flex-direction: column;
  z-index: 100;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}
.userMenu button {
  background: transparent;
  border: none;
  font-family: inherit;
  font-size: 0.9rem;
  color: var(--color-text);
  text-align: left;
  padding: 10px 14px;
  cursor: pointer;
}
.userMenu button:hover { background: var(--color-surface-deep); }
```

- [ ] **Step 3: Verify types**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/chrome/Nav.tsx frontend/src/components/chrome/Nav.module.css
git commit -m "feat(chrome): auth-aware nav (sign in/up vs user menu)"
```

---

### Task 56: HomePage + NightCta — guard the CTAs

**Files:**
- Modify: `frontend/src/pages/HomePage.tsx`
- Modify: `frontend/src/components/home/NightCta.tsx`

- [ ] **Step 1: Patch HomePage CTA handler**

In `HomePage.tsx`, add `import { useAuth } from '../lib/AuthContext'` and `import { useAuthModal } from '../lib/AuthModalContext'`. Inside `HomePage()`:

```tsx
  const { user } = useAuth()
  const { openAuth } = useAuthModal()
  const tryClick = (e: React.MouseEvent) => {
    e.preventDefault()
    if (!user) openAuth('signIn'); else openModal()
    handleRipple(e)
  }
```

Replace the existing CTA `onClick={(e) => { e.preventDefault(); openModal(); handleRipple(e) }}` with `onClick={tryClick}`.

- [ ] **Step 2: Patch NightCta**

In `NightCta.tsx`, replace `onClick={openModal}` (or its equivalent) with the same `tryClick` pattern. Add the same imports and helper at the top of the component.

- [ ] **Step 3: Verify types**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/HomePage.tsx frontend/src/components/home/NightCta.tsx
git commit -m "feat(frontend): home + nightCta CTAs route through auth modal when signed out"
```

---

### Task 57: StoryModal — verify-email panel

**Files:**
- Modify: `frontend/src/components/modal/StoryModal.tsx`
- Modify: `frontend/src/components/modal/StoryModal.module.css`

- [ ] **Step 1: Add a `Phase = 'form' | 'creating' | 'verify'` branch**

At the top of `StoryModal.tsx`, add `import { useAuth } from '../../lib/AuthContext'`. Inside the component:

```tsx
  const { user, resendVerification } = useAuth()
  const needsVerify = !!user && !user.emailVerified
  const [resendDone, setResendDone] = useState(false)
```

Replace the body branch when `phase === 'form'` so that, if `needsVerify`, it renders a verify panel instead of the form:

```tsx
          {phase === 'form' && needsVerify && (
            <div className={styles.verifyPanel}>
              <h2 className={styles.creatingTitle}>{t.auth.messages.verifyPanelTitle}</h2>
              <p className={styles.creatingHint}>{t.auth.messages.verifyPanelBody(user!.email)}</p>
              <button
                className={styles.resendBtn}
                disabled={resendDone}
                onClick={async () => { await resendVerification(); setResendDone(true) }}
              >
                {resendDone ? '✓' : t.auth.actions.resend}
              </button>
            </div>
          )}
          {phase === 'form' && !needsVerify && (
            <StoryForm onSubmit={handleSubmit} loading={false} inModal />
          )}
```

- [ ] **Step 2: Add `.verifyPanel` and `.resendBtn` styles to `StoryModal.module.css`**

```css
.verifyPanel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  text-align: center;
  padding: 16px 20px 28px;
}
.resendBtn {
  background: var(--color-magic, #7C3AED);
  color: #fff;
  border: none;
  border-radius: 12px;
  padding: 10px 18px;
  font-family: inherit;
  font-size: 0.95rem;
  cursor: pointer;
}
.resendBtn:disabled { opacity: 0.6; cursor: default; }
```

- [ ] **Step 3: Verify types**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/modal/StoryModal.tsx frontend/src/components/modal/StoryModal.module.css
git commit -m "feat(modal): show verify-email panel when user is unverified"
```

---

### Task 58: EmailVerifiedPage

**Files:**
- Create: `frontend/src/pages/EmailVerifiedPage.tsx`
- Create: `frontend/src/pages/EmailVerifiedPage.module.css`

- [ ] **Step 1: Page**

```tsx
import { useSearchParams, useNavigate } from 'react-router-dom'
import { useLocale } from '../lib/LocaleContext'
import { useAuth } from '../lib/AuthContext'
import { useAuthModal } from '../lib/AuthModalContext'
import { useEffect } from 'react'
import styles from './EmailVerifiedPage.module.css'

export function EmailVerifiedPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const { user, refresh } = useAuth()
  const { openAuth } = useAuthModal()
  const { t } = useLocale()
  const ok = params.get('ok') === '1'

  useEffect(() => { if (ok) refresh() }, [ok, refresh])

  return (
    <div className={styles.page}>
      <h1 className={styles.heading}>
        {ok ? t.auth.messages.verifySuccess : t.auth.messages.verifyError}
      </h1>
      <button
        className={styles.btn}
        onClick={() => user ? navigate('/stories') : openAuth('signIn')}
      >
        {user ? t.auth.actions.myArchive : t.auth.tabs.signIn}
      </button>
    </div>
  )
}
```

- [ ] **Step 2: CSS**

```css
.page {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  gap: 20px;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  text-align: center;
}
.heading { max-width: 600px; font-size: clamp(1.4rem, 3vw, 2rem); }
.btn {
  background: linear-gradient(90deg, var(--color-ember, #D97706), var(--color-magic, #7C3AED));
  color: #fff; border: none; border-radius: 999px;
  padding: 12px 24px; font-family: inherit; font-size: 1rem; cursor: pointer;
}
```

- [ ] **Step 3: Verify types**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/EmailVerifiedPage.tsx frontend/src/pages/EmailVerifiedPage.module.css
git commit -m "feat(pages): EmailVerifiedPage"
```

---

### Task 59: PasswordResetPage

**Files:**
- Create: `frontend/src/pages/PasswordResetPage.tsx`
- Create: `frontend/src/pages/PasswordResetPage.module.css`

- [ ] **Step 1: Page**

```tsx
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'
import { useAuthModal } from '../lib/AuthModalContext'
import { useLocale } from '../lib/LocaleContext'
import { ApiError } from '../lib/types'
import styles from './PasswordResetPage.module.css'

export function PasswordResetPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const { confirmPasswordReset } = useAuth()
  const { openAuth } = useAuthModal()
  const { t } = useLocale()
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)

  if (!token) {
    return <div className={styles.page}><h1>{t.auth.errors.TOKEN_INVALID}</h1></div>
  }

  if (done) {
    return (
      <div className={styles.page}>
        <h1>{t.auth.messages.passwordUpdated}</h1>
        <button className={styles.btn} onClick={() => openAuth('signIn')}>
          {t.auth.tabs.signIn}
        </button>
      </div>
    )
  }

  async function handle(e: React.FormEvent) {
    e.preventDefault()
    if (password !== confirm) { setError(t.auth.errors.passwordMismatch); return }
    if (password.length < 8) { setError(t.auth.errors.passwordTooShort); return }
    setSubmitting(true)
    try {
      await confirmPasswordReset(token, password)
      setDone(true)
    } catch (err) {
      const code = err instanceof ApiError ? err.body.error : 'ERROR'
      setError(t.auth.errors[code as keyof typeof t.auth.errors] ?? t.auth.errors.ERROR)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className={styles.page}>
      <h1 className={styles.heading}>{t.auth.actions.submitReset}</h1>
      <form onSubmit={handle} className={styles.form}>
        {error && <div className={styles.banner}>{error}</div>}
        <input className={styles.input} type="password" required minLength={8}
               placeholder={t.auth.fields.newPassword}
               value={password} onChange={e => setPassword(e.target.value)} disabled={submitting} />
        <input className={styles.input} type="password" required minLength={8}
               placeholder={t.auth.fields.confirmPassword}
               value={confirm} onChange={e => setConfirm(e.target.value)} disabled={submitting} />
        <button className={styles.btn} type="submit" disabled={submitting}>
          {submitting ? '…' : t.auth.actions.submitReset}
        </button>
      </form>
    </div>
  )
}
```

- [ ] **Step 2: CSS**

```css
.page {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  gap: 18px;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
}
.heading { font-size: 1.6rem; }
.form { display: flex; flex-direction: column; gap: 12px; width: 100%; max-width: 360px; }
.input {
  border: 1px solid var(--color-surface-deep);
  background: var(--color-surface);
  color: var(--color-text);
  border-radius: 10px;
  padding: 11px 13px;
  font-family: inherit;
  font-size: 0.95rem;
}
.btn {
  background: linear-gradient(90deg, var(--color-ember, #D97706), var(--color-magic, #7C3AED));
  color: #fff; border: none; border-radius: 12px;
  padding: 12px 18px; font-family: inherit; font-size: 1rem; cursor: pointer;
}
.banner { background: #FEE2E2; color: #991B1B; padding: 10px 14px; border-radius: 8px; font-size: 0.88rem; }
```

- [ ] **Step 3: Verify types**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/PasswordResetPage.tsx frontend/src/pages/PasswordResetPage.module.css
git commit -m "feat(pages): PasswordResetPage"
```

---

### Task 60: AdminUsersPage

**Files:**
- Create: `frontend/src/pages/AdminUsersPage.tsx`
- Create: `frontend/src/pages/AdminUsersPage.module.css`

- [ ] **Step 1: Page**

```tsx
import { useEffect, useState } from 'react'
import { admin } from '../lib/apiClient'
import type { AdminUser } from '../lib/apiClient'
import styles from './AdminUsersPage.module.css'

export function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUser[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    admin.listUsers().then(setUsers).catch(e => setError(String(e)))
  }, [])

  if (error) return <p className={styles.msg}>{error}</p>
  if (!users) return <p className={styles.msg}>...</p>

  return (
    <div className={styles.page}>
      <h1 className={styles.heading}>Users ({users.length})</h1>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>Email</th><th>Name</th><th>Role</th>
            <th>Verified</th><th>Google</th><th>Created</th><th>Stories</th>
          </tr>
        </thead>
        <tbody>
          {users.map(u => (
            <tr key={u.id}>
              <td>{u.email}</td>
              <td>{u.displayName}</td>
              <td>{u.role}</td>
              <td>{u.emailVerified ? '✓' : '✗'}</td>
              <td>{u.googleLinked ? '✓' : '✗'}</td>
              <td>{new Date(u.createdAt).toLocaleString()}</td>
              <td>{u.storyCount}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
```

- [ ] **Step 2: CSS**

```css
.page { padding: 32px 24px; max-width: 1100px; margin: 0 auto; }
.heading { margin-bottom: 16px; }
.msg { padding: 32px; text-align: center; }
.table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }
.table th, .table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--color-surface-deep);
  text-align: left;
}
.table th { color: var(--color-text-muted); font-weight: 600; }
```

- [ ] **Step 3: Verify types**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/AdminUsersPage.tsx frontend/src/pages/AdminUsersPage.module.css
git commit -m "feat(pages): AdminUsersPage"
```

---

## Phase K — Integration + verification

### Task 61: App.tsx — providers, routes, Google redirect handling

**Files:**
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Replace App.tsx**

```tsx
import { useEffect, useRef } from 'react'
import { BrowserRouter, Routes, Route, useLocation, useNavigate } from 'react-router-dom'
import { ThemeProvider } from './lib/ThemeContext'
import { LocaleProvider } from './lib/LocaleContext'
import { StoryModalProvider } from './lib/StoryModalContext'
import { AuthProvider, useAuth } from './lib/AuthContext'
import { AuthModalProvider } from './lib/AuthModalContext'
import { StoryModal } from './components/modal/StoryModal'
import { AuthModal } from './components/auth/AuthModal'
import { Nav } from './components/chrome/Nav'
import { Footer } from './components/chrome/Footer'
import { RequireAuth } from './components/auth/RequireAuth'
import { RequireAdmin } from './components/auth/RequireAdmin'
import { HomePage } from './pages/HomePage'
import { ArchivePage } from './pages/ArchivePage'
import { StoryDetailPage } from './pages/StoryDetailPage'
import { EmailVerifiedPage } from './pages/EmailVerifiedPage'
import { PasswordResetPage } from './pages/PasswordResetPage'
import { AdminUsersPage } from './pages/AdminUsersPage'

function ScrollProgress() {
  const barRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const bar = barRef.current
    if (!bar) return
    const update = () => {
      const scrollable = document.body.scrollHeight - window.innerHeight
      const pct = scrollable > 0 ? (window.scrollY / scrollable) * 100 : 0
      bar.style.width = pct + '%'
    }
    window.addEventListener('scroll', update, { passive: true })
    return () => window.removeEventListener('scroll', update)
  }, [])
  return <div ref={barRef} className="scrollProgress" />
}

function CursorTrail() {
  useEffect(() => {
    const colors = ['#C4B5FD', '#EDD9A3', '#D97706', '#7C3AED', '#F59E0B']
    let lastX = 0, lastY = 0, lastTime = 0
    const onMove = (e: MouseEvent) => {
      const now = Date.now()
      const dx = e.clientX - lastX
      const dy = e.clientY - lastY
      if (now - lastTime < 40 || Math.sqrt(dx * dx + dy * dy) < 20) return
      lastX = e.clientX
      lastY = e.clientY
      lastTime = now
      const size = 8 + Math.random() * 10
      const color = colors[Math.floor(Math.random() * colors.length)]
      const star = document.createElement('div')
      star.className = 'cursorStar'
      star.innerHTML = `<svg width="${size}" height="${size}" viewBox="0 0 16 16" fill="none"><path d="M8 0L9.5 6.5L16 8L9.5 9.5L8 16L6.5 9.5L0 8L6.5 6.5Z" fill="${color}"/></svg>`
      star.style.left = (e.clientX - size / 2 + (Math.random() - 0.5) * 10) + 'px'
      star.style.top = (e.clientY - size / 2 + (Math.random() - 0.5) * 10) + 'px'
      document.body.appendChild(star)
      setTimeout(() => star.remove(), 800)
    }
    document.addEventListener('mousemove', onMove)
    return () => document.removeEventListener('mousemove', onMove)
  }, [])
  return null
}

function GoogleAuthLanding() {
  const location = useLocation()
  const navigate = useNavigate()
  const { refresh } = useAuth()
  useEffect(() => {
    const params = new URLSearchParams(location.search)
    if (params.get('auth') === 'ok') {
      params.delete('auth')
      const search = params.toString()
      navigate({ pathname: location.pathname, search: search ? '?' + search : '' }, { replace: true })
      refresh()
    }
  }, [location, navigate, refresh])
  return null
}

function AppShell() {
  return (
    <>
      <ScrollProgress />
      <CursorTrail />
      <GoogleAuthLanding />
      <Nav />
      <main>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/stories" element={<RequireAuth><ArchivePage /></RequireAuth>} />
          <Route path="/stories/:id" element={<RequireAuth><StoryDetailPage /></RequireAuth>} />
          <Route path="/verify-email" element={<EmailVerifiedPage />} />
          <Route path="/reset-password" element={<PasswordResetPage />} />
          <Route path="/admin/users" element={<RequireAdmin><AdminUsersPage /></RequireAdmin>} />
        </Routes>
      </main>
      <Footer />
      <StoryModal />
      <AuthModal />
    </>
  )
}

export default function App() {
  return (
    <ThemeProvider>
      <LocaleProvider>
        <BrowserRouter>
          <AuthProvider>
            <AuthModalProvider>
              <StoryModalProvider>
                <AppShell />
              </StoryModalProvider>
            </AuthModalProvider>
          </AuthProvider>
        </BrowserRouter>
      </LocaleProvider>
    </ThemeProvider>
  )
}
```

- [ ] **Step 2: Verify types and lint**

Run: `cd frontend && node_modules/.bin/tsc --noEmit && npm run lint`
Expected: clean.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/App.tsx
git commit -m "feat(app): wire AuthProvider, routes, Google ?auth=ok hydration"
```

---

### Task 62: Manual smoke verification

This task has no commits — it's a checklist of manual flows to run before declaring the work done.

- [ ] **Step 1: Start the stack**

```bash
docker compose down -v
docker compose up -d --build
```

- [ ] **Step 2: Anonymous browse**

- Visit `http://localhost/`. Hero, How it works, Features, Story preview, Night CTA all render.
- Click "Try" CTA → AuthModal opens on Sign in tab.
- Visit `http://localhost/stories` directly → modal opens, content gated.

- [ ] **Step 3: Email/password sign up + verification**

- Sign up via the modal with a real email (Gmail SMTP env configured).
- Modal closes, nav shows display name dropdown.
- Try "Try" CTA → StoryModal opens with the verify panel (not the form).
- Open the verification email, click the link → land on `/verify-email?ok=1`.
- Refresh; "Try" now opens the form.

- [ ] **Step 4: Sign out + sign in**

- Sign out from the dropdown → nav reverts to Sign in / Sign up buttons.
- Open the modal Sign in tab → log in with the same email/password → success.

- [ ] **Step 5: Forgot password**

- Sign out. Open modal → "Forgot password?" → submit email → 204.
- Open the email → click link → land on `/reset-password?token=…` → submit new password.
- Sign in with the new password → success.

- [ ] **Step 6: Google OAuth**

- Configure `GOOGLE_CLIENT_ID/SECRET` in `.env` (real Google Cloud client).
- Click "Continue with Google" → Google consent → land back on `/?auth=ok` (param stripped).
- Nav shows the user. `email_verified` is true (no panel in StoryModal).

- [ ] **Step 7: Story scoping**

- Sign up as user A (verify), create a story.
- Sign out, sign up as user B (verify). `/stories` is empty.
- Manually visit `/stories/<A-story-id>` → 404.

- [ ] **Step 8: Admin**

- Set `ADMIN_EMAIL=admin@kazka.local` and `ADMIN_PASSWORD=admin12345` in `.env`, restart backend.
- Sign in as that admin → nav dropdown shows "Admin → Users".
- Visit `/admin/users` → table renders, response in DevTools has no `passwordHash` or `googleSubject`.
- As admin, visit `/stories/<A-story-id>` → renders.

- [ ] **Step 9: Session invalidation on password reset**

- Open two browser windows, sign in as the same user.
- In window 1, request a password reset → confirm with new password.
- In window 2, click any nav action → next API call returns 401, you're signed out.

If any step fails, file the issue and patch the relevant code; do not mark the plan complete until all steps pass.

---

## Self-review checklist (run after implementation)

- All sections in the spec map to a task above.
- No "TBD" / "TODO" / "Implement later" left in the file.
- Every code step has the actual code.
- Type names are consistent: `User`, `UserDto`, `UserRole`, `CurrentUser`, `KazkaUserDetails`, `AdminUser`, `ApiError`, `AuthErrorCode`.
- File paths are absolute under repo root.



