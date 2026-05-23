# syntax=docker/dockerfile:1.7

# ── Build stage ─────────────────────────────────────────────────────────────────
FROM dhi.io/node:24 AS builder
WORKDIR /app

# Install pnpm
RUN corepack enable && corepack prepare pnpm@11.2.2 --activate

COPY package.json pnpm-lock.yaml* ./
RUN --mount=type=cache,target=/pnpm/store,sharing=locked \
    pnpm config set store-dir /pnpm/store \
    && pnpm install --frozen-lockfile

COPY . .
RUN --mount=type=cache,target=/pnpm/store,sharing=locked \
    pnpm config set store-dir /pnpm/store \
    && pnpm build \
    && test -f .output/server/index.mjs \
    || (echo "ERROR: pnpm build did not produce .output/server/index.mjs" && exit 1)

LABEL org.opencontainers.image.authors="Patrick Aziken <onaziken@gmail.com>"

# ── Runtime stage ───────────────────────────────────────────────────────────────
FROM dhi.io/node:24
WORKDIR /app

RUN addgroup -S audita && adduser -S audita -G audita

COPY --from=builder --chown=audita:audita /app/.output ./

USER audita
EXPOSE 3000
ENV NODE_ENV=production
CMD ["node", "server/index.mjs"]
