import api from "../api.js";

export const payWithCOD = (data) =>
    api.post('/orders',data);

export const payWithMomo = (data) =>
    api.post('/orders/momo', data);

export const getOrderById = (id) =>
    api.get(`/orders/${id}`);

export const notifyMomoPaymentStatus = (data) =>
    api.post("/payments/momo/notify", data)

export const getOrderByUser = (params) => {
    const queryString = new URLSearchParams(params).toString();
    return api.get(`/orders?${queryString}`)
}

export const getOrderStats = () =>
    api.get('/orders/stats')

export const cancelOrder = (data) =>
    api.post(`/orders/cancel-order`, data);