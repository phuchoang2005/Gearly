import tailwindcss from "@tailwindcss/vite";
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
    plugins: [
        react(),
        tailwindcss()
    ],
    define: {
        global: 'window'
    },
    resolve: {
        alias: {
            // Public
            '@assets': path.resolve(__dirname, './src/assets'),
            '@contexts': path.resolve(__dirname, './src/contexts'),
            '@routes': path.resolve(__dirname, './src/routes'),
            '@utils': path.resolve(__dirname, './src/utils'),
            '@data': path.resolve(__dirname, './public/data'),

            // User
            '@u_components': path.resolve(__dirname, './src/components/user'),
            '@u_hooks': path.resolve(__dirname, './src/hooks/user'),
            '@u_layouts': path.resolve(__dirname, './src/layouts/user'),
            '@u_pages': path.resolve(__dirname, './src/pages/user'),
            '@u_services': path.resolve(__dirname, './src/services/user'),

            // Admin
            '@a_components': path.resolve(__dirname, './src/components/admin'),
            '@a_hooks': path.resolve(__dirname, './src/hooks/admin'),
            '@a_layouts': path.resolve(__dirname, './src/layouts/admin'),
            '@a_pages': path.resolve(__dirname, './src/pages/admin'),
            '@a_services': path.resolve(__dirname, './src/services/admin'),
        },
    },
    server: {
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
        },
    },
})
