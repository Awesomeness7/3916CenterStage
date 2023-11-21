// vite.config.js
import { resolve } from 'path'
import { defineConfig } from 'vite'

// Tree shaking doesn't seem to work with plotly.js and it increases build times,
// so let's stick with plotly.js-dist-min.

export default defineConfig({
  build: {
    rollupOptions: {
      input: {
        index: resolve(__dirname, 'toml/index.html'),
      },
    },
  },
})