import api from '../api';

export const getWishlist = (params) => {
    const queryString = new URLSearchParams(params).toString();
    return api.get(`/wishlist?${queryString}`);
}

export const addToWishlist = (productId) => api.post(`/wishlist/${productId}`);

export const removeFromWishlist = (productId) => api.delete(`/wishlist/${productId}`);

export const mergeWishlist = (productIds) => api.post('/wishlist/batch', productIds);

export const bulkRemoveWishlist = (productids) => api.post('/wishlist/bulk_remove', [...productids])