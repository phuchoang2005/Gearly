import api from "../api.js";

export const fetchStaticPageBySlug = async (slug) => {
    const response = await api.get(`/pages/${slug}`);
    return response.data;
}