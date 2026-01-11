import api from '../api';

export const getWishlist = (params) => {
    const queryString = new URLSearchParams(params).toString();
    return api.get(`/wishlist?${queryString}`);
}

export const addToWishlist = (bookId) => api.post(`/wishlist/${bookId}`);

export const removeFromWishlist = (bookId) => api.delete(`/wishlist/${bookId}`);

export const mergeWishlist = (bookIds) => api.post('/wishlist/batch', bookIds);

export const bulkRemoveWishlist = (bookids) => api.post('/wishlist/bulk_remove', [...bookids])