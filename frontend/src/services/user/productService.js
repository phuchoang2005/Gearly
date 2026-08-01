import api from '../api';

export const getBestProducts = () => api.get('/products/bestByRating');

export const getProducts = async (params) => {
    const queryString = new URLSearchParams(params).toString();
    return api.get(`/products/search?${queryString}`);
};

export const getProductDetails = async (productId) => {
    return api.get(`/products/${productId}`);
}

export const getProductByIds = async (productIds, searchTxt, pageIndex, pageSize) => {
    return api.get(`/products?ids=${productIds}&searchTxt=${searchTxt}&pageIndex=${pageIndex}&pageSize=${pageSize}`);
}