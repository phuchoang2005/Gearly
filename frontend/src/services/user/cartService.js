import api from '../api';

// Guest endpoints
export const initGuestCart = () =>
    api.post('/guest-cart/init').then(res => res.data.guestId);

export const getGuestCart = (guestId) =>
    api.get(`/guest-cart?guestId=${guestId}`);

export const addGuestItem = (guestId, item) =>
    api.post(`/guest-cart/add?guestId=${guestId}`, item);

export const updateGuestQuantity = (guestId, bookId, quantity) =>
    api.put(
        `/guest-cart/update?guestId=${guestId}&bookId=${bookId}&quantity=${quantity}`
    );

export const removeGuestItem = (guestId, bookId) =>
    api.delete(`/guest-cart/remove?guestId=${guestId}&bookId=${bookId}`);

export const clearGuestCart = (guestId) =>
    api.delete(`/guest-cart/clear?guestId=${guestId}`);

export const addGuestItems = (guestId, items) =>
    api.post(`/guest-cart/bulk-add?guestId=${guestId}`, items);

// Authenticated endpoints
export const getCart = () =>
    api.get('/cart');

export const addItem = (item) =>
    api.post('/cart/add', item);

export const updateQuantity = (bookId, quantity) =>
    api.put(`/cart/update?bookId=${bookId}&quantity=${quantity}`);

export const removeItem = (bookId) =>
    api.delete(`/cart/remove?bookId=${bookId}`);

export const clearCart = () =>
    api.delete('/cart/clear');

export const mergeCart = (guestId, items) =>
    api.post(`/cart/merge?guestId=${guestId}`, items);

export const deleteGuestCart = (guestId) =>
    api.delete(`/cart/guest-cart?guestId=${guestId}`);

export const addItems = (items) =>
    api.post(`/cart/bulk-add`, items);
