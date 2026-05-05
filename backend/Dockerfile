# ================================================================
# Multi-stage build — keeps the final image small.
#
# Stage 1 (build): has Maven + JDK to compile the app.
# Stage 2 (run):   has only the JRE + the compiled JAR.
#
# Why this matters: Maven + JDK adds ~500 MB. The final image
# only needs the JRE (~200 MB) to run the JAR. Smaller images
# pull faster and have a smaller attack surface in production.
# ================================================================

# ── Stage 1: Build ────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first and download dependencies as a separate layer.
# Docker caches each layer. If pom.xml hasn't changed, this step
# is skipped on the next build — saving minutes of download time.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source and build. Because this is a separate layer from
# the dependency download, changing source code only rebuilds from here.
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Run ──────────────────────────────────────────────
FROM eclipse-temurin:17-jre

# Non-root user — never run apps as root inside containers
RUN groupadd spring && useradd -r -g spring spring
USER spring:spring

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# JVM tuning for containers:
#   -XX:+UseContainerSupport  → respect container memory limits (not host RAM)
#   -XX:MaxRAMPercentage=75.0 → use up to 75% of the container's memory for heap
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]

EXPOSE 8080
