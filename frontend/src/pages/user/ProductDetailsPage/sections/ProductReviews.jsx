import { useRef } from "react";
import RatingStar from "@u_components/products/RatingStar.jsx";
import PrecomputePagination from "@u_components/shared/PrecomputePagination.jsx";

export default function ProductReviews({
    averageRating,
    ratingCount,
    productReview,
    ratingDistribution,
    state,
    setState
}) {
    const reviewListRef = useRef(null);
    let { number = 0, totalPages = 0, content: reviews = [] } = productReview || {};

    return (
        <div className="max-w-6xl mx-auto my-16">
            {/* Header */}
            <div className="flex justify-between items-center mb-8">
                <h2 className="text-xl font-semibold text-black">
                    Customer Reviews
                </h2>
                {Number(averageRating) > 0 && (
                    <span className="font-semibold text-[#D70018]">
                        {averageRating} out of 5
                    </span>
                )}
            </div>

            {/* Summary & Filter */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">
                {/* Rating Distribution */}
                <div className="bg-white border border-gray-300 rounded-lg p-6">
                    <h3 className="text-lg font-semibold mb-6 text-black">
                        Rating Distribution
                    </h3>
                    <div className="space-y-4">
                        {ratingDistribution.map(({ stars, count, percentage }) => (
                            <div key={stars} className="flex items-center space-x-2">
                                <div className="w-16 text-sm font-medium text-gray-700">
                                    {stars} star
                                </div>
                                <div className="flex-1 bg-gray-200 h-3 rounded-full overflow-hidden">
                                    <div
                                        className="bg-[#D70018] h-full rounded-full"
                                        style={{ width: `${percentage}%` }}
                                    />
                                </div>
                                <div className="w-8 text-right text-gray-600">
                                    {count}
                                </div>
                            </div>
                        ))}
                    </div>
                    <div className="mt-6 text-sm text-gray-600">
                        Based on {ratingCount} {ratingCount > 1 ? "reviews" : "review"}
                    </div>
                </div>

                {/* Filter Buttons */}
                <div className="bg-white border border-gray-300 rounded-lg p-6">
                    <h3 className="text-lg font-semibold mb-6 text-black">
                        Filter Reviews
                    </h3>
                    <div className="flex flex-wrap gap-3">
                        {["all", 5, 4, 3, 2, 1].map((value) => {
                            const ratingValue = typeof value === "string" ? 0 : value;
                            const isActive = state.rating === ratingValue;

                            return (
                                <button
                                    key={value}
                                    onClick={() =>
                                        setState((prev) => ({
                                            ...prev,
                                            rating: ratingValue,
                                            pageIndex: 0
                                        }))
                                    }
                                    className={`px-4 py-2 rounded-full border text-sm transition ${
                                        isActive
                                            ? "bg-[#D70018] text-white border-[#D70018]"
                                            : "bg-white text-gray-700 border-gray-300 hover:bg-gray-100"
                                    }`}
                                >
                                    {value === "all" ? "All Reviews" : `${value} Star`}
                                </button>
                            );
                        })}
                    </div>
                </div>
            </div>

            {reviews.length === 0 ? (
                <div className="text-center text-gray-600 text-sm py-12 border border-dashed border-gray-300 rounded-md bg-white">
                    No reviews available.
                </div>
            ) : (
                <>
                    {/* Review List */}
                    <div className="space-y-4 mb-10" ref={reviewListRef}>
                        {[...reviews]
                            .sort(
                                (a, b) =>
                                    new Date(b.addedAt).getTime() -
                                    new Date(a.addedAt).getTime()
                            )
                            .map(
                                ({ id, rating, userName, addedAt, subject, comment }) => (
                                    <div
                                        key={id}
                                        className="border border-gray-300 rounded-md p-8 bg-white"
                                    >
                                        <div className="flex items-start justify-between mb-2">
                                            <RatingStar ratingValue={rating} />
                                            <div className="flex items-center space-x-4">
                                                <span className="font-medium text-black">
                                                    {userName}
                                                </span>
                                                <span className="text-gray-500 text-sm">
                                                    {new Date(addedAt).toLocaleDateString(
                                                        "en-US",
                                                        {
                                                            year: "numeric",
                                                            month: "short",
                                                            day: "numeric"
                                                        }
                                                    )}
                                                </span>
                                            </div>
                                        </div>

                                        {subject || comment ? (
                                            <>
                                                <h3 className="font-semibold text-base mb-3 text-black">
                                                    {subject || "No subject"}
                                                </h3>
                                                <p className="text-gray-700 text-sm">
                                                    {comment || "No comment provided."}
                                                </p>
                                            </>
                                        ) : (
                                            <p className="text-gray-500 text-sm italic">
                                                No detailed feedback.
                                            </p>
                                        )}
                                    </div>
                                )
                            )}
                    </div>

                    {/* Pagination */}
                    <PrecomputePagination
                        totalPages={totalPages}
                        currentPage={number + 1}
                        setState={setState}
                        scrollRef={reviewListRef}
                    />
                </>
            )}
        </div>
    );
}
