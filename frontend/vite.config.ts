import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react({
      babel: {
        plugins: [["babel-plugin-react-compiler"]],
      },
    }),
    tailwindcss(),
  ],
  // Proxy /api calls to the backend during local development to avoid CORS issues
  server: {
    host: true, // allow access from mobile devices on the same network
    proxy: {
      "/api": {
        target: process.env.VITE_DEV_API ?? "http://127.0.0.1:8080",
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
