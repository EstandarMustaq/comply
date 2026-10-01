# Build stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Security: non-root user
RUN addgroup -S fincomp && adduser -S fincomp -G fincomp

COPY --from=builder --chown=fincomp:fincomp /build/target/grc-engine-*.jar app.jar
RUN mkdir -p /var/lib/fincomp/imports && chown -R fincomp:fincomp /var/lib/fincomp

USER fincomp
EXPOSE 8080

# Tuned JVM parameters for EC2 t4g.small (2GB RAM)
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=70.0 -XX:InitialRAMPercentage=40.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
