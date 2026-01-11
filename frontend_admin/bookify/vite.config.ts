import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: [
      // Whenever something tries to import "rc-util/node_modules/react-is",
      // redirect it to your top‐level react-is package instead.
      {
        find: /\/rc-util\/node_modules\/react-is/,
        replacement: path.resolve(__dirname, "node_modules/react-is"),
      },
      // (Optionally) ensure any other nested react‐is is the same:
      {
        find: /react-is\/cjs\/react-is\.development\.js/,
        replacement: path.resolve(__dirname, "node_modules/react-is/index.js"),
      },
    ],
  },
});
