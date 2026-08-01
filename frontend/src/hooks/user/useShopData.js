import {useQueries} from '@tanstack/react-query'
import {getProducts} from '@u_services/productService'
import {getCategories} from '@u_services/categoryService'

export const useShopData = (queryParams) => {
    const [categoriesQ, productsQ] = useQueries({
        queries: [
            {
                queryKey: ['categories'],
                queryFn: () => getCategories().then(r => r.data),
                staleTime: 1000 * 60 * 30
            },
            {
                queryKey: ['shopData', queryParams],
                queryFn: () => getProducts(queryParams).then(r => r.data),
            },
        ]
    })

    return {
        categories: categoriesQ.data,
        products: productsQ.data,
        isLoading: categoriesQ.isLoading || productsQ.isLoading,
        isError: categoriesQ.isError || productsQ.isError,
    }
}
