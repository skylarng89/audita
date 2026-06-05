import tailwindcss from "@tailwindcss/vite";

const debug = process.env.DEBUG === "true";

export default defineNuxtConfig({
  devtools: { enabled: debug },
  ssr: false,

  modules: ["@pinia/nuxt", "@nuxt/eslint", "nuxt-security"],

  routeRules: {
    "/api/**": {
      security: {
        xssValidator: false,
      },
    },
  },

  security: {
    csrf: false,
    nonce: true,
    headers: {
      contentSecurityPolicy: {
        "default-src": ["'self'"],
        "base-uri": ["'none'"],
        "form-action": ["'self'"],
        "frame-ancestors": ["'none'"],
        "object-src": ["'none'"],
        "img-src": ["'self'", "'data:'", "blob:", "https:"],
        "font-src": ["'self'", "'data:'", "https://fonts.gstatic.com"],
        "style-src": ["'self'", "'nonce-{{nonce}}'", "https://fonts.googleapis.com"],
        "script-src": ["'self'", "'nonce-{{nonce}}'"],
        "script-src-attr": ["'none'"],
        "connect-src": ["'self'"],
        "upgrade-insecure-requests": true,
      },
      referrerPolicy: "no-referrer",
      strictTransportSecurity: {
        maxAge: 15552000,
        includeSubdomains: true,
      },
      xContentTypeOptions: "nosniff",
      xFrameOptions: "DENY",
      xXSSProtection: "0",
      crossOriginOpenerPolicy: "same-origin",
      crossOriginResourcePolicy: "same-origin",
    },
    allowedMethodsRestricter: {
      methods: ["GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
      throwError: true,
    },
  },

  css: ["flatpickr/dist/flatpickr.min.css", "~/assets/css/main.css"],

  sourcemap: {
    client: debug,
    server: debug,
  },

  vite: {
    plugins: [tailwindcss()],
  },

  runtimeConfig: {
    apiInternalBase: process.env.NUXT_API_INTERNAL_BASE,
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE ?? "",
      apiContractVersion:
        process.env.NUXT_PUBLIC_API_CONTRACT_VERSION ?? "2026-05-12-auth-v2",
    },
  },

  app: {
    head: {
      title: "Audita — Change Management",
      meta: [
        { charset: "utf-8" },
        { name: "viewport", content: "width=device-width, initial-scale=1" },
        {
          name: "description",
          content:
            "Sovereign Architect of Infrastructure. ITIL/ITSM Change Management.",
        },
      ],
      link: [
        {
          rel: "icon",
          type: "image/svg+xml",
          href: "/brand/audita-icon-light.svg",
          media: "(prefers-color-scheme: light)",
        },
        {
          rel: "icon",
          type: "image/svg+xml",
          href: "/brand/audita-icon-dark.svg",
          media: "(prefers-color-scheme: dark)",
        },
        {
          rel: "icon",
          type: "image/svg+xml",
          href: "/brand/audita-icon-light.svg",
        },
        { rel: "preconnect", href: "https://fonts.googleapis.com" },
        {
          rel: "stylesheet",
          href: "https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap",
        },
      ],
    },
  },

  typescript: {
    strict: true,
    typeCheck: false,
  },

  compatibilityDate: "2025-04-01",
});
