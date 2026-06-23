export default {
  darkMode: "class",
  content: [
    "./components/**/*.{vue,ts}",
    "./layouts/**/*.vue",
    "./pages/**/*.vue",
    "./plugins/**/*.ts",
    "./composables/**/*.ts",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"],
        mono: ["JetBrains Mono", "ui-monospace", "monospace"],
      },
      colors: {
        primary: {
          DEFAULT: "#00236f",
          dark: "#001a52",
          light: "#1e3a8a",
          container: "#1e3a8a",
          fixed: "#dce1ff",
        },
        surface: {
          DEFAULT: "#f7f9fb",
          bright: "#f7f9fb",
          dark: "#1E293B",
          container: "#eceef0",
          "container-low": "#f2f4f6",
          "container-high": "#e6e8ea",
          "container-highest": "#e0e3e5",
          "container-lowest": "#ffffff",
          dim: "#d8dadc",
          variant: "#e0e3e5",
        },
        "on-surface": "#191c1e",
        "on-surface-variant": "#444651",
        secondary: {
          DEFAULT: "#505f76",
          container: "#d0e1fb",
        },
        muted: "#757682",
        border: {
          DEFAULT: "#c5c5d3",
          dark: "#334155",
        },
        danger: {
          DEFAULT: "#ba1a1a",
          light: "#ffdad6",
          border: "#ff897d",
        },
        warning: {
          DEFAULT: "#F59E0B",
          light: "#FFFBEB",
        },
        success: {
          DEFAULT: "#10B981",
          light: "#ECFDF5",
        },
        info: {
          DEFAULT: "#3B82F6",
          light: "#EFF6FF",
        },
        outline: "#757682",
        "outline-variant": "#c5c5d3",
        dm: {
          base: "var(--c-base)",
          chrome: "var(--c-chrome)",
          surface: "var(--c-surface)",
          "surface-raised": "var(--c-surface-raised)",
          "surface-highest": "var(--c-surface-highest)",
          input: "var(--c-input)",
          border: "var(--c-border)",
          "border-subtle": "var(--c-border-subtle)",
        },
      },
      backgroundImage: {
        "hero-gradient": "linear-gradient(150deg, #00236f 0%, #1e3a8a 100%)",
      },
      boxShadow: {
        card: "0 1px 3px 0 rgba(0,35,111,0.06), 0 1px 2px -1px rgba(0,35,111,0.04)",
        "card-hover":
          "0 4px 12px -2px rgba(0,35,111,0.10), 0 2px 4px -2px rgba(0,35,111,0.06)",
        "card-md": "0 4px 24px rgba(0,35,111,0.08)",
      },
      borderRadius: {
        DEFAULT: "0.5rem",
        lg: "0.75rem",
        xl: "1rem",
      },
    },
  },
  plugins: [],
};
