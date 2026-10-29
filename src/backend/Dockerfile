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

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 wikicook
WORKDIR /app
COPY --from=build /build/app.jar app.jar
USER wikicook
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
