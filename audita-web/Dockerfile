# syntax=docker/dockerfile:1.7

# ── Build stage ─────────────────────────────────────────────────────────────────
FROM dhi.io/node:24-dev AS builder
WORKDIR /app

RUN corepack enable && corepack prepare pnpm@11.2.2 --activate

COPY package.json pnpm-lock.yaml* pnpm-workspace.yaml ./
RUN --mount=type=cache,target=/pnpm/store,sharing=locked \
    pnpm config set store-dir /pnpm/store \
    && pnpm install --frozen-lockfile --ignore-scripts

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

COPY --from=builder --chown=65532:65532 /app/.output ./

USER 65532:65532
EXPOSE 3000
ENV NODE_ENV=production
CMD ["node", "server/index.mjs"]
