import {useQueries} from '@tanstack/react-query'
import {getBookDetails} from "@u_services/bookService.js";
import {getBookReviews, getRatingDistribution} from "@u_services/reviewService.js";

export const useBookDetailsData = (bookId, reviewParams) => {
    const [bookDetailsQ, bookReviewsQ, ratingDistributionQ] = useQueries({
        queries: [
            {
                queryKey: ['bookDetails', bookId],
                queryFn: () => getBookDetails(bookId).then(r => r.data),
            },
            {
                queryKey: ['bookReviews', reviewParams],
                queryFn: () => getBookReviews(reviewParams).then(r => r.data),
                keepPreviousData: true,
            },
            {
                queryKey: ['ratingDistribution', bookId],
                queryFn: () => getRatingDistribution(bookId).then(r => r.data),
            },
        ]
    })

    return {
        bookDetails: bookDetailsQ.data,
        bookReviews: bookReviewsQ.data,
        ratingDistribution: ratingDistributionQ.data,
        isLoading: bookDetailsQ.isLoading || bookReviewsQ.isLoading || ratingDistributionQ.isLoading,
        isError: bookDetailsQ.isError || bookReviewsQ.isError || ratingDistributionQ.isError,
    }
}
