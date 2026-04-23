import type { Config } from 'tailwindcss'

export default {
  darkMode: 'class',
  content: [
    './components/**/*.{vue,ts}',
    './layouts/**/*.vue',
    './pages/**/*.vue',
    './plugins/**/*.ts',
    './composables/**/*.ts',
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'monospace'],
      },
      colors: {
        primary: {
          DEFAULT: '#1D3A8A',
          dark: '#152D6E',
          light: '#2B4EAD',
        },
        surface: {
          DEFAULT: '#FFFFFF',
          dark: '#1E293B',
        },
        muted: '#6B7280',
        border: {
          DEFAULT: '#E5E7EB',
          dark: '#334155',
        },
        danger: {
          DEFAULT: '#EF4444',
          light: '#FEF2F2',
          border: '#FCA5A5',
        },
        warning: {
          DEFAULT: '#F59E0B',
          light: '#FFFBEB',
        },
        success: {
          DEFAULT: '#10B981',
          light: '#ECFDF5',
        },
        info: {
          DEFAULT: '#3B82F6',
          light: '#EFF6FF',
        },
      },
      backgroundImage: {
        'hero-gradient': 'linear-gradient(135deg, #1D3A8A 0%, #152D6E 100%)',
      },
      boxShadow: {
        card: '0 1px 3px 0 rgba(0,0,0,0.08), 0 1px 2px -1px rgba(0,0,0,0.06)',
        'card-hover': '0 4px 6px -1px rgba(0,0,0,0.08), 0 2px 4px -2px rgba(0,0,0,0.06)',
      },
      borderRadius: {
        DEFAULT: '0.5rem',
      },
    },
  },
  plugins: [],
} satisfies Config
