/**
 * Theme colours for code that can't use CSS variables (e.g. Chart.js canvas drawing).
 * Keep in sync with the custom properties in purple-black-theme.css.
 */
export const palette = {
  primary: '#977cc7', // --bs-primary
  primaryDark: '#5a2d9e', // --bs-primary-dark
  primaryDarker: '#4a2c7a', // --bs-primary-darker
  primaryLight: '#68478e', // --bs-primary-light
  primaryLighter: '#7657a6', // --bs-primary-lighter
  primaryLightest: '#b19ed1', // --bs-primary-lightest
  secondary: '#503280', // --bs-secondary
  accent: '#8e44ad', // --bs-accent
  textLight: '#e6d9f2', // --bs-text-light
  textMuted: '#b8a9d9', // --bs-text-muted
  white: '#ffffff',
} as const;
