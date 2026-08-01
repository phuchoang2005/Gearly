import api from "../api";

export const getProductReviews = (queryParams) => {
    const params = new URLSearchParams(queryParams);
    return api.get(`/reviews?${params}`);
}

export const getRatingDistribution = (productId) => {
    return api.get(`/reviews/distribution?productId=${productId}`);
}

export const submitReview = (data) =>
    api.post(`/reviews/submit-review`, data)

export const getTopReviews = () =>
    api.get(`/reviews/best-six`)