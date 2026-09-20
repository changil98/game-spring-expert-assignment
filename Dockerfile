# ---- 1단계: 빌드 ----
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Gradle wrapper와 설정 파일 먼저 복사 (의존성 캐싱 활용)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 의존성만 먼저 받아서 레이어 캐싱
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 후 빌드
COPY src src
RUN ./gradlew clean build -x test --no-daemon

# ---- 2단계: 실행 ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]