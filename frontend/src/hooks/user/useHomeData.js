import {useQueries} from '@tanstack/react-query'
import {getBestProducts} from '@u_services/productService'
import {getCategories} from '@u_services/categoryService'
import {getTopReviews} from "@u_services/reviewService.js";

export const useHomeData = () => {
    const [categoriesQ, bestProductsQ, bestReviewsQ] = useQueries({
        queries: [
            {
                queryKey: ['categories'],
                queryFn: () => getCategories().then(r => r.data),
                staleTime: 1000 * 60 * 30,      // 30 minutes
                cacheTime: 1000 * 60 * 60,     // 1 hour
            },
            {
                queryKey: ['bestProducts'],
                queryFn: () => getBestProducts().then(r => r.data),
            },
            {
                queryKey: ['bestReviews'],
                queryFn: () => getTopReviews().then(r => r.data)
            }
        ]
    })

    return {
        categories: categoriesQ.data,
        bestProducts: bestProductsQ.data,
        bestReviews: bestReviewsQ.data,
        isLoading: categoriesQ.isLoading || bestProductsQ.isLoading || bestReviewsQ.isLoading,
        isError: categoriesQ.isError || bestProductsQ.isError || bestReviewsQ.isError,
    }
}
