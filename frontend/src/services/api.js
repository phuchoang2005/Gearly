import axios from 'axios';

const api = axios.create({
    baseURL: '/api',
    timeout: 10000,
});

api.interceptors.request.use((config) => {
    const stored = localStorage.getItem("auth");
    if (stored) {
        const token = JSON.parse(stored).token;
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
    }
    return config;
});

export default api;
