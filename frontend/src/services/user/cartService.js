import api from '../api';

// Guest endpoints
export const initGuestCart = () =>
    api.post('/guest-cart/init').then(res => res.data.guestId);

export const getGuestCart = (guestId) =>
    api.get(`/guest-cart?guestId=${guestId}`);

export const addGuestItem = (guestId, item) =>
    api.post(`/guest-cart/add?guestId=${guestId}`, item);

export const updateGuestQuantity = (guestId, productId, quantity) =>
    api.put(
        `/guest-cart/update?guestId=${guestId}&productId=${productId}&quantity=${quantity}`
    );

export const removeGuestItem = (guestId, productId) =>
    api.delete(`/guest-cart/remove?guestId=${guestId}&productId=${productId}`);

export const clearGuestCart = (guestId) =>
    api.delete(`/guest-cart/clear?guestId=${guestId}`);

export const addGuestItems = (guestId, items) =>
    api.post(`/guest-cart/bulk-add?guestId=${guestId}`, items);

// Authenticated endpoints
export const getCart = () =>
    api.get('/cart');

export const addItem = (item) =>
    api.post('/cart/add', item);

export const updateQuantity = (productId, quantity) =>
    api.put(`/cart/update?productId=${productId}&quantity=${quantity}`);

export const removeItem = (productId) =>
    api.delete(`/cart/remove?productId=${productId}`);

export const clearCart = () =>
    api.delete('/cart/clear');

export const mergeCart = (guestId, items) =>
    api.post(`/cart/merge?guestId=${guestId}`, items);

export const deleteGuestCart = (guestId) =>
    api.delete(`/cart/guest-cart?guestId=${guestId}`);

export const addItems = (items) =>
    api.post(`/cart/bulk-add`, items);
