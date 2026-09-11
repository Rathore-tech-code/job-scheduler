/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        base: '#0E1117',
        panel: '#161B24',
        raised: '#1D2330',
        hairline: '#2A3142',
        ink: '#E7E9EE',
        muted: '#8993A8',
        faint: '#5B6478',
        amber: '#E8A33D',
        mint: '#3DDC97',
        coral: '#E86A5D',
        slate: '#6C7BA6'
      },
      fontFamily: {
        display: ['"Space Grotesk"', 'sans-serif'],
        body: ['"Inter"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace']
      },
      borderRadius: {
        sm: '4px',
        DEFAULT: '6px'
      }
    }
  },
  plugins: []
}
