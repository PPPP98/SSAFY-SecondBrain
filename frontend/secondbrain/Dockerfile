# ---------- 1) Build stage: Vite build with pnpm (Node 24) ----------
FROM node:24-alpine AS builder

# 일부 패키지 호환 대비
RUN apk add --no-cache libc6-compat

WORKDIR /app

# pnpm 활성화 (Corepack)
RUN corepack enable
# 필요 시 버전 고정 (잠그고 싶다면 주석 해제해서 사용)
# RUN corepack prepare pnpm@9 --activate

# 의존성 캐시 최적화
COPY pnpm-lock.yaml package.json ./
RUN pnpm install --frozen-lockfile

# 소스 복사 & 프로덕션 빌드
COPY . .
RUN pnpm build

# ---------- 2) Runtime stage: Nginx serve ----------
FROM nginx:1.27-alpine AS runner

# 정적 파일 복사
COPY --from=builder /app/dist /usr/share/nginx/html

# 기본 conf 교체 (SPA 라우팅 지원)
RUN rm -f /etc/nginx/conf.d/default.conf && \
    printf '%s\n' \
    'server {' \
    '  listen 80;' \
    '  server_name _;' \
    '  root /usr/share/nginx/html;' \
    '  index index.html;' \
    '  location / {' \
    '    try_files $uri $uri/ /index.html;' \
    '  }' \
    '  location = /health {' \
    '    return 200 "ok";' \
    '    add_header Content-Type text/plain;' \
    '  }' \
    '}' \
    > /etc/nginx/conf.d/app.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost/health || exit 1