# ── Build stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY src ./src

# Compile all Java sources into /app/out
RUN mkdir -p out && \
    find src -name "*.java" | xargs javac -d out

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/out ./out
EXPOSE 8080
CMD ["java", "-cp", "out", "com.sandbox.Main"]
