import {useQueries} from '@tanstack/react-query'
import {getProductDetails} from "@u_services/productService.js";
import {getProductReviews, getRatingDistribution} from "@u_services/reviewService.js";

export const useProductDetailsData = (productId, reviewParams) => {
    const [productDetailsQ, productReviewsQ, ratingDistributionQ] = useQueries({
        queries: [
            {
                queryKey: ['productDetails', productId],
                queryFn: () => getProductDetails(productId).then(r => r.data),
            },
            {
                queryKey: ['productReviews', reviewParams],
                queryFn: () => getProductReviews(reviewParams).then(r => r.data),
                keepPreviousData: true,
            },
            {
                queryKey: ['ratingDistribution', productId],
                queryFn: () => getRatingDistribution(productId).then(r => r.data),
            },
        ]
    })

    return {
        productDetails: productDetailsQ.data,
        productReviews: productReviewsQ.data,
        ratingDistribution: ratingDistributionQ.data,
        isLoading: productDetailsQ.isLoading || productReviewsQ.isLoading || ratingDistributionQ.isLoading,
        isError: productDetailsQ.isError || productReviewsQ.isError || ratingDistributionQ.isError,
    }
}
