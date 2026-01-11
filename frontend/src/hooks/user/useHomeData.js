import {useQueries} from '@tanstack/react-query'
import {getBestBooks} from '@u_services/bookService'
import {getCategories} from '@u_services/categoryService'
import {getTopReviews} from "@u_services/reviewService.js";

export const useHomeData = () => {
    const [categoriesQ, bestBooksQ, bestReviewsQ] = useQueries({
        queries: [
            {
                queryKey: ['categories'],
                queryFn: () => getCategories().then(r => r.data),
                staleTime: 1000 * 60 * 30,      // 30 minutes
                cacheTime: 1000 * 60 * 60,     // 1 hour
            },
            {
                queryKey: ['bestBooks'],
                queryFn: () => getBestBooks().then(r => r.data),
            },
            {
                queryKey: ['bestReviews'],
                queryFn: () => getTopReviews().then(r => r.data)
            }
        ]
    })

    return {
        categories: categoriesQ.data,
        bestBooks: bestBooksQ.data,
        bestReviews: bestReviewsQ.data,
        isLoading: categoriesQ.isLoading || bestBooksQ.isLoading || bestReviewsQ.isLoading,
        isError: categoriesQ.isError || bestBooksQ.isError || bestReviewsQ.isError,
    }
}
