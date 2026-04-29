export default defineNuxtConfig({
  devtools: { enabled: true },
  ssr: false,

  modules: ["@nuxtjs/tailwindcss", "@pinia/nuxt", "@nuxt/eslint"],

  tailwindcss: {
    cssPath: "~/assets/css/main.css",
  },

  runtimeConfig: {
    apiInternalBase: process.env.NUXT_API_INTERNAL_BASE ?? "http://api:8080",
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE ?? "/api",
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
        { rel: "icon", type: "image/svg+xml", href: "/favicon.svg" },
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
