# syntax=docker/dockerfile:1
# Builds wikicook-app from the multi-module project (context: src/backend). Tests run in CI, not here.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY wikicook-db wikicook-db
COPY wikicook-ai wikicook-ai
COPY wikicook-app wikicook-app
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl wikicook-app -am package -DskipTests \
    && cp wikicook-app/target/wikicook-app-*.jar /build/app.jar

# Split the fat jar into Spring Boot layers so a code change only rebuilds the small application layer.
FROM eclipse-temurin:21-jre-alpine AS extract
WORKDIR /build
COPY --from=build /build/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S -g 1001 wikicook && adduser -S -u 1001 -G wikicook wikicook
WORKDIR /app
# Least to most frequently changed.
COPY --from=extract /build/extracted/dependencies/ ./
COPY --from=extract /build/extracted/spring-boot-loader/ ./
COPY --from=extract /build/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/extracted/application/ ./
USER wikicook
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=10 \
    CMD wget -qO /dev/null http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", \
    "org.springframework.boot.loader.launch.JarLauncher"]
