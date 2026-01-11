import { Cpu } from "lucide-react";
import BookSlider from "@u_components/products/BookSlider.jsx";

export default function FeatureBookSection({ books }) {
    return (
        <section className="relative w-full pt-16 text-center" aria-label="Featured Products">
            <h2 className="inline-flex items-center gap-3 px-6 py-2 border border-[#D70018] rounded-full text-[#D70018] font-semibold uppercase tracking-wide text-lg bg-white shadow-md">
                <Cpu className="w-5 h-5 md:w-6 md:h-6" />
                Featured PC Parts
            </h2>
            <div className="mt-4 w-24 mx-auto h-1 bg-[#D70018] rounded-full shadow-sm" />
            <div className="mt-10">
                <BookSlider books={books} />
            </div>
        </section>
    );
}
