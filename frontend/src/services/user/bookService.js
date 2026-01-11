import api from '../api';

export const getBestBooks = () => api.get('/books/bestByRating');

export const getBooks = async (params) => {
    const queryString = new URLSearchParams(params).toString();
    return api.get(`/books/search?${queryString}`);
};

export const getBookDetails = async (bookId) => {
    return api.get(`/books/${bookId}`);
}

export const getBookByIds = async (bookIds, searchTxt, pageIndex, pageSize) => {
    return api.get(`/books?ids=${bookIds}&searchTxt=${searchTxt}&pageIndex=${pageIndex}&pageSize=${pageSize}`);
}