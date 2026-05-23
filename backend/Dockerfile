FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle/ gradle/
RUN chmod +x gradlew

COPY build.gradle settings.gradle ./
COPY src/ src/

RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app

# Run as non-root: drop privileges before exec'ing the JVM so RCE in the app
# does not become root in the container.
RUN groupadd --system --gid 1001 kazka \
 && useradd  --system --uid 1001 --gid kazka --no-create-home --shell /sbin/nologin kazka

COPY --from=build --chown=kazka:kazka /workspace/build/libs/kazka-backend-0.1.0.jar app.jar

# Default uploads dir (overridden by UPLOADS_DIR env var). Ensure it's writable by the
# runtime user. If the deployment mounts a volume here, the volume must be writable
# by uid 1001 (chown the host path, or set fsGroup in Kubernetes).
RUN mkdir -p /app/uploads && chown -R kazka:kazka /app/uploads
VOLUME /app/uploads

USER kazka:kazka
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
