/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        usc: {
          cardinal: '#9D2235',
          gold: '#FFC72C',
        },
      },
    },
  },
  plugins: [],
};
