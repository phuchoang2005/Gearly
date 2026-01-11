import {useQuery} from '@tanstack/react-query';
import {getWishlist} from '@u_services/wishlistService';
import {getBookByIds} from "@u_services/bookService.js";

export const useWishlistData = (auth, queryParams, setQueryParams) => {
    return useQuery({
        queryKey: ['wishlist', queryParams],
        queryFn: async () => {
            let res = null;
            if (!auth) {
                const wishlistItems = JSON.parse(localStorage.getItem("wishlist") || "[]");

                if (wishlistItems.length === 0) return [];
                res = await getBookByIds(wishlistItems, queryParams.searchTxt, queryParams.pageIndex, queryParams.pageSize);
            } else {
                res = await getWishlist(queryParams);
            }
            if (res.data.empty && queryParams.pageIndex > 0) {
                setQueryParams(prev => ({
                    ...prev,
                    pageIndex: prev.pageIndex - 1
                }));
            }
            return res.data;
        },
        keepPreviousData: true
    });
};
