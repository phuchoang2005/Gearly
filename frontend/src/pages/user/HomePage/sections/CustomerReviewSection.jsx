import { useState } from "react";
import { MessageSquare, CheckCircle } from "lucide-react";
import "@smastrom/react-rating/style.css";
import RatingStar from "@u_components/products/RatingStar.jsx";
import React from "react";

export default function CustomerReviewSection({ reviews }) {
    return (
        <section className="relative w-full py-16 px-4 bg-gray-50" aria-label="Customer Reviews">
            <div className="text-center mb-10">
                <h2
                    className="inline-flex items-center gap-3 px-8 py-3 border-2 border-transparent rounded-full text-white font-bold uppercase tracking-wide text-xl
                    bg-gradient-to-r from-[#D70018] to-[#B80012] shadow-lg hover:from-[#B80012] hover:to-[#D70018]
                    transition duration-300 ease-in-out transform hover:scale-105 cursor-pointer"
                >
                    <MessageSquare className="w-6 h-6 text-white" />
                    What Our Customers Say
                </h2>
                <p className="text-gray-600 max-w-2xl mx-auto mt-4">
                    Real feedback from customers about their PC builds and components.
                </p>
            </div>

            <div className="max-w-7xl mx-auto">
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {reviews.map((review) => (
                        <ReviewCard key={review.id} review={review} />
                    ))}
                </div>
            </div>
        </section>
    );
}

function ReviewCard({ review }) {
    const [expanded, setExpanded] = useState(false);
    const toggleExpanded = () => setExpanded((prev) => !prev);
    const displayText = expanded ? review.comment : review.comment.slice(0, 200);

    return (
        <div className="bg-white rounded-lg shadow-md p-6 transition-transform transform hover:scale-105 hover:shadow-lg border border-gray-200 cursor-pointer">
            <div className="flex items-start mb-4">
                <div className="w-12 h-12 rounded-full overflow-hidden mr-4 border border-gray-300">
                    <img
                        src={"/data/User Avatars/user-avatar.jpg"}
                        alt={review.userName}
                        className="w-full h-full object-cover"
                    />
                </div>
                <div>
                    <h3 className="font-semibold text-gray-800">{review.userName}</h3>
                    <div className="flex items-center mt-1">
                        <RatingStar ratingValue={review.rating} />
                        <span className="ml-2 text-sm text-gray-500">{review.addedAt}</span>
                    </div>
                </div>
            </div>

            <h4 className="text-gray-800 font-medium mb-2">{review.subject}</h4>
            <p className="text-gray-600 text-sm leading-relaxed mb-3">
                {displayText}
                {review.comment.length > 200 && (
                    <button
                        onClick={toggleExpanded}
                        className="ml-1 text-sm text-[#D70018] underline hover:text-[#B80012]"
                    >
                        {expanded ? "Show less" : "Read more"}
                    </button>
                )}
            </p>

            <div className="flex items-center gap-1 pt-2 border-t border-gray-100">
                <CheckCircle className="w-4 h-4 text-green-600" />
                <span className="text-xs text-gray-600">Verified Purchase</span>
            </div>
        </div>
    );
}
