# ---------- 1) Build stage ----------
FROM node:24-alpine AS builder
RUN apk add --no-cache libc6-compat
WORKDIR /app

# 👇 빌드 시 넘길 앱 경로 (기본값: 현재 디렉터리)
ARG APP_SUBDIR=.

ENV PNPM_HOME=/usr/local/share/pnpm
ENV PATH=$PNPM_HOME:$PATH
RUN corepack enable && corepack prepare pnpm@9 --activate

# 의존성 설치(캐시 최적화)
COPY ${APP_SUBDIR}/package.json ${APP_SUBDIR}/pnpm-lock.yaml ./

RUN pnpm install --frozen-lockfile

# 소스만 복사
COPY ${APP_SUBDIR}/ ./

# 빌드 (tanstack router 코드젠 포함)
RUN pnpm build
RUN test -f ./src/routeTree.gen.ts || (echo >&2 "❌ routeTree.gen.ts가 생성되지 않았습니다"; exit 1)
RUN test -d ./dist || (echo >&2 "❌ dist 디렉터리가 없습니다"; exit 1)

# ---------- 2) Runtime stage ----------
FROM nginx:1.27-alpine AS runner
COPY --from=builder /app/dist /usr/share/nginx/html
RUN rm -f /etc/nginx/conf.d/default.conf && \
    printf '%s\n' \
    'server {' \
    '  listen 80;' \
    '  server_name _;' \
    '  root /usr/share/nginx/html;' \
    '  index index.html;' \
    '  location ~* \.(?:js|css|png|jpg|jpeg|gif|svg|ico|woff2?)$ { try_files $uri =404; add_header Cache-Control "public, max-age=31536000, immutable"; }' \
    '  location / { try_files $uri $uri/ /index.html; }' \
    '  location = /health { return 200 "ok"; add_header Content-Type text/plain; }' \
    '}' > /etc/nginx/conf.d/app.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -qO- http://localhost/health || exit 1
