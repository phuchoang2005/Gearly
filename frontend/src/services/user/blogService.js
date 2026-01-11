import api from "../api.js";

export const fetchBlogSummaries = async () => {
    const response = await api.get(`/blogposts`);
    return response.data;
};

export const fetchBlogDetails = async (id) => {
    const response = await api.get(`/blogposts/${id}`);
    return response.data;
};