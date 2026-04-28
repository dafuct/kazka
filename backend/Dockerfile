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
COPY --from=build /workspace/build/libs/kazka-backend-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
