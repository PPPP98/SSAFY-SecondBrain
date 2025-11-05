# syntax=docker/dockerfile:1.6

# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# 테스트 스킵 여부를 빌드 인자로 제어
ARG SKIP_TESTS=true

# Gradle 래퍼/의존성만 먼저 복사해서 캐시 극대화
COPY gradlew settings.gradle* build.gradle* gradle.properties* ./
COPY gradle ./gradle
# 실행권한 + CRLF 방지
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew

# BuildKit 캐시를 이용해 Gradle 캐시 보존
RUN --mount=type=cache,target=/root/.gradle ./gradlew --version

# 이후 소스 복사
COPY . .

# 실행권한 + CRLF 방지
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew

# 의존성/컴파일 캐시 활용 + 테스트 스킵 옵션
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew clean bootJar $( [ "$SKIP_TESTS" = "true" ] && echo "-x test" )

# 실행 JAR 하나 확정 복사 준비
RUN set -eu; \
    JAR_FILE="$(ls build/libs/*.jar | grep -v 'plain' | head -n 1)"; \
    [ -n "$JAR_FILE" ] || { echo "No runnable JAR found"; exit 1; }; \
    cp "$JAR_FILE" /app/app.jar

# Runtime stage
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# healthcheck용 curl 설치
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 비루트 유저
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring

# 빌드 산출물 복사
COPY --from=build /app/app.jar /app/app.jar

# 포트 노출 및 옵션
EXPOSE 8080
ENV JAVA_OPTS=""

# 엔트리포인트
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
