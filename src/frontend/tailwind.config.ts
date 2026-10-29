import type { Config } from 'tailwindcss';

// Theme values point at the Soft Editorial CSS variables (src/styles/tokens.css), so cook themes switch for free.
const color = (name: string) => `var(--color-${name})`;

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    colors: {
      transparent: 'transparent',
      current: 'currentColor',
      bg: color('bg'),
      surface: { DEFAULT: color('surface'), alt: color('surface-alt') },
      border: { DEFAULT: color('border'), strong: color('border-strong') },
      ink: { DEFAULT: color('ink'), muted: color('ink-muted') },
      primary: {
        DEFAULT: color('primary'),
        strong: color('primary-strong'),
        tint: color('primary-tint'),
        on: color('on-primary'),
      },
      warm: { DEFAULT: color('warm'), strong: color('warm-strong'), tint: color('warm-tint'), on: color('on-warm') },
      saffron: { DEFAULT: color('saffron'), tint: color('saffron-tint') },
      success: { DEFAULT: color('success'), tint: color('success-tint'), on: color('on-success') },
      danger: { DEFAULT: color('danger'), tint: color('danger-tint'), on: color('on-danger') },
    },
    fontFamily: {
      display: ['var(--font-display)'],
      body: ['var(--font-body)'],
    },
    borderRadius: {
      none: '0',
      sm: 'var(--radius-sm)',
      md: 'var(--radius-md)',
      lg: 'var(--radius-lg)',
      pill: 'var(--radius-pill)',
    },
    boxShadow: {
      none: 'none',
      1: 'var(--shadow-1)',
      2: 'var(--shadow-2)',
    },
    extend: {
      spacing: {
        'ds-1': 'var(--space-1)',
        'ds-2': 'var(--space-2)',
        'ds-3': 'var(--space-3)',
        'ds-4': 'var(--space-4)',
        'ds-5': 'var(--space-5)',
        'ds-6': 'var(--space-6)',
        'ds-7': 'var(--space-7)',
        'ds-8': 'var(--space-8)',
        'touch-sm': 'var(--touch-sm)',
        'touch-md': 'var(--touch-md)',
        'touch-lg': 'var(--touch-lg)',
      },
      maxWidth: {
        content: 'var(--content-max)',
        reading: 'var(--reading-max)',
      },
    },
  },
  plugins: [],
} satisfies Config;
