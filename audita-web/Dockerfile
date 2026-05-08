# ── Build stage ─────────────────────────────────────────────────────────────────
FROM node:24.15.0-alpine3.22 AS builder
WORKDIR /app

# Install pnpm
RUN corepack enable && corepack prepare pnpm@10.11.0 --activate

COPY package.json pnpm-lock.yaml* ./
RUN pnpm install --frozen-lockfile

COPY . .
RUN pnpm build && test -f .output/server/index.mjs || (echo "ERROR: pnpm build did not produce .output/server/index.mjs" && exit 1)

# ── Runtime stage ───────────────────────────────────────────────────────────────
FROM node:24.15.0-alpine3.22
WORKDIR /app

RUN addgroup -S audita && adduser -S audita -G audita

COPY --from=builder --chown=audita:audita /app/.output ./

USER audita
EXPOSE 3000
ENV NODE_ENV=production
CMD ["node", "server/index.mjs"]
