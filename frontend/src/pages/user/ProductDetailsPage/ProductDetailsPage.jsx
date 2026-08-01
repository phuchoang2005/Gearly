import { useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { useProductDetailsData } from "@u_hooks/useProductDetailsData.js";
import LoadingScreen from "@u_components/shared/LoadingScreen.jsx";
import ErrorScreen from "@u_components/shared/ErrorScreen.jsx";
import HeaderBreadcrumb from "@u_components/shared/HeaderBreadcrumb.jsx";
import ProductDetails from "@u_pages/ProductDetailsPage/sections/ProductDetails.jsx";
import ProductReviews from "@u_pages/ProductDetailsPage/sections/ProductReviews.jsx";

export default function ProductDetailsPage() {
    const { productId } = useParams();
    const [state, setState] = useState({
        productId,
        rating: 0,
        pageIndex: 0,
        pageSize: 5
    });

    const { productDetails, productReviews, ratingDistribution, isLoading, isError } = useProductDetailsData(productId, state);

    const productDetailsComp = useMemo(() => (
        <>
            <HeaderBreadcrumb
                title="Product Details"
                crumbs={[
                    { name: "Home", path: "/" },
                    { name: "Shop", path: "/shop" },
                    { name: "Details", path: `/product/${productId}` }
                ]}
            />
            <ProductDetails productDetails={productDetails} />
        </>
    ), [productId, productDetails]);

    return (
        <>
            {isLoading ? (
                <LoadingScreen />
            ) : isError ? (
                <ErrorScreen />
            ) : (
                <>
                    {productDetailsComp}
                    <ProductReviews
                        averageRating={productDetails?.averageRating}
                        ratingCount={productDetails?.ratingCount}
                        productReview={productReviews}
                        ratingDistribution={ratingDistribution}
                        state={state}
                        setState={setState}
                    />
                </>
            )}
        </>
    );
}
