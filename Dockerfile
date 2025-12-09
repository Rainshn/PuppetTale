# Build Stage
FROM gradle:8.7-jdk17 AS builder
WORKDIR /app

# 소스 전체 복사
COPY . .

# gradle로 직접 빌드
RUN gradle clean build -x test --no-daemon

# Run Stage
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# 빌드 결과 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]