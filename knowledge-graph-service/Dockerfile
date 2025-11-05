# ===== Stage 1: Builder =====
FROM python:3.11-slim as builder

WORKDIR /app

# uv 설치
RUN pip install --no-cache-dir uv

# pyproject.toml 복사
COPY pyproject.toml .

# uv로 패키지 설치 (프로덕션 의존성만)
RUN uv sync --no-dev


# ===== Stage 2: Runtime =====
FROM python:3.11-slim

WORKDIR /app

# 타임존 및 헬스 체크 도구 설치
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        tzdata && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 빌드 스테이지에서 생성한 virtual environment 복사
COPY --from=builder /app/.venv /app/.venv

# 환경변수 설정
ENV PATH=/app/.venv/bin:$PATH
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1
ENV TZ=Asia/Seoul

# 소스 코드 복사
COPY . .

# 포트 노출
EXPOSE 8000

# 앱 실행
CMD ["python", "main.py"]