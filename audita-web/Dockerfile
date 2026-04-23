# ── Build stage ─────────────────────────────────────────────────────────────────
FROM node:22-alpine AS builder
WORKDIR /app

# Install pnpm
RUN corepack enable && corepack prepare pnpm@latest --activate

COPY package.json pnpm-lock.yaml* ./
RUN pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

# ── Runtime stage ───────────────────────────────────────────────────────────────
FROM node:22-alpine
WORKDIR /app

RUN addgroup -S audita && adduser -S audita -G audita

COPY --from=builder --chown=audita:audita /app/.output ./

USER audita
EXPOSE 3000
ENV NODE_ENV=production
CMD ["node", "server/index.mjs"]
