# 1단계: Build Stage
FROM gradle:8.7-jdk17 AS builder
WORKDIR /app

# Gradle 캐싱을 위해 build 파일 복사
COPY build.gradle settings.gradle gradlew /app/
COPY gradle /app/gradle

RUN ./gradlew --version

# 소스코드 전체 복사
COPY . /app

# JAR 빌드
RUN ./gradlew clean build -x test

# Run Stage (Slim JDK)
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# 빌드 결과 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# Render 포트
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]