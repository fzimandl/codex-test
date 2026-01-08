# syntax=docker/dockerfile:1.7

# -------- Build stage --------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom first for dependency download layer caching
COPY pom.xml ./

# Pre-fetch dependencies for better layer caching
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests dependency:go-offline

# Copy sources and build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests clean package spring-boot:repackage

# -------- Runtime stage --------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create non-root user
RUN useradd -r -u 1001 appuser

# Copy built jar (match artifact id and any version classifier)
COPY --from=build /app/target/coinmate-streamer-*.jar /app/app.jar

# Set environment defaults (can be overridden)
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=""

EXPOSE 8081

USER appuser

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
