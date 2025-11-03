# ---------- Build stage ----------
FROM node:24-alpine AS builder
WORKDIR /app
RUN apk add --no-cache libc6-compat && corepack enable

# 프론트 디렉터리만 복사 (예: frontend/secondbrain)
# 경로는 실제 레포 구조에 맞게 바꾸세요
COPY frontend/secondbrain/pnpm-lock.yaml frontend/secondbrain/package.json ./
ENV HUSKY=0
RUN pnpm install --frozen-lockfile

COPY frontend/secondbrain/ ./
RUN pnpm build

# ---------- Runtime (nginx) ----------
FROM nginx:1.27-alpine AS runner
# 빌드 산출물 경로는 Vite 기본값: /app/dist
COPY --from=builder /app/dist /usr/share/nginx/html
# 기본 80포트로 서빙
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
