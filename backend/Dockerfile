# securehr/api image (Section 0.11). Multi-stage: build with the JDK, run on the JRE only.

# ---- Build stage -------------------------------------------------------------------------
FROM eclipse-temurin:21.0.13_9-jdk-noble AS build
WORKDIR /src

# Resolve dependencies in their own layer so source edits don't re-download them.
COPY .mvn .mvn
COPY mvnw pom.xml checkstyle-suppressions.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

# Lint and tests run in their own Jenkins stages (Section 11.2); the image stage only packages.
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q package -DskipTests -Dspotless.check.skip=true -Dcheckstyle.skip=true \
 && java -Djarmode=tools -jar target/securehr-api.jar extract --layers --launcher --destination /extracted

# ---- Runtime stage -----------------------------------------------------------------------
FROM eclipse-temurin:21.0.13_9-jre-noble

# Dedicated unprivileged user; the container never runs as root.
RUN groupadd --system --gid 10001 securehr \
 && useradd --system --uid 10001 --gid securehr --no-create-home --shell /usr/sbin/nologin securehr

WORKDIR /app
# Least-frequently-changing layers first for smaller image updates.
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./

USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
