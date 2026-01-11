import { useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { useBookDetailsData } from "@u_hooks/useBookDetailsData.js";
import LoadingScreen from "@u_components/shared/LoadingScreen.jsx";
import ErrorScreen from "@u_components/shared/ErrorScreen.jsx";
import HeaderBreadcrumb from "@u_components/shared/HeaderBreadcrumb.jsx";
import BookDetails from "@u_pages/BookDetailsPage/sections/BookDetails.jsx";
import BookReviews from "@u_pages/BookDetailsPage/sections/BookReviews.jsx";

export default function BookDetailsPage() {
    const { bookId } = useParams();
    const [state, setState] = useState({
        bookId,
        rating: 0,
        pageIndex: 0,
        pageSize: 5
    });

    const { bookDetails, bookReviews, ratingDistribution, isLoading, isError } = useBookDetailsData(bookId, state);

    const bookDetailsComp = useMemo(() => (
        <>
            <HeaderBreadcrumb
                title="Book Details"
                crumbs={[
                    { name: "Home", path: "/" },
                    { name: "Shop", path: "/shop" },
                    { name: "Details", path: `/book/${bookId}` }
                ]}
            />
            <BookDetails bookDetails={bookDetails} />
        </>
    ), [bookId, bookDetails]);

    return (
        <>
            {isLoading ? (
                <LoadingScreen />
            ) : isError ? (
                <ErrorScreen />
            ) : (
                <>
                    {bookDetailsComp}
                    <BookReviews
                        averageRating={bookDetails?.averageRating}
                        ratingCount={bookDetails?.ratingCount}
                        bookReview={bookReviews}
                        ratingDistribution={ratingDistribution}
                        state={state}
                        setState={setState}
                    />
                </>
            )}
        </>
    );
}
