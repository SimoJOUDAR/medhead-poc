# syntax=docker/dockerfile:1.7

# Multi-stage build: stage 1 compiles the application with a JDK image, stage 2
# runs the produced JAR on a slim JRE image. Dependency resolution is isolated
# in its own layer so source-only edits do not invalidate the Maven download
# cache.

ARG JDK_IMAGE=eclipse-temurin:17-jdk-jammy
ARG JRE_IMAGE=eclipse-temurin:17-jre-jammy

FROM ${JDK_IMAGE} AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
 && ./mvnw -B -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests package \
 && cp target/medhead-poc-*.jar target/app.jar

FROM ${JRE_IMAGE} AS runtime
WORKDIR /app

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 1001 app \
 && useradd  --system --uid 1001 --gid 1001 --home /app --shell /usr/sbin/nologin app

COPY --from=build --chown=app:app /workspace/target/app.jar /app/app.jar

USER app
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl --fail --silent http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
