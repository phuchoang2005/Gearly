import { Star } from "lucide-react";
import BookSlider from "@u_components/products/BookSlider.jsx";

export default function BestSellerSection({ books }) {
    return (
        <section className="relative w-full py-16 text-center bg-[#FFF5F5]" aria-label="Best Sellers">
            <h2 className="inline-flex items-center gap-3 px-6 py-2 border border-[#D70018] text-[#D70018] bg-white rounded-full font-semibold uppercase tracking-wide text-lg shadow-md">
                <Star className="w-5 h-5 md:w-6 md:h-6 text-[#D70018] fill-current" />
                Best Sellers
            </h2>
            <div className="mt-4 w-24 mx-auto h-1 bg-[#D70018] rounded-full shadow-sm" />
            <div className="mt-10">
                <BookSlider books={books} />
            </div>
        </section>
    );
}
