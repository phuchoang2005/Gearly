import { useState } from "react";
import { useKeenSlider } from "keen-slider/react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import ProductCard from "./ProductCard.jsx";

const BookSlider = ({ books }) => {
    const [currentSlide, setCurrentSlide] = useState(0);
    const [loaded, setLoaded] = useState(false);

    const [sliderRef, instanceRef] = useKeenSlider(
        {
            loop: true,
            renderMode: "performance",
            slides: {
                perView: 4,
                spacing: 16,
            },
            breakpoints: {
                "(max-width: 768px)": {
                    slides: { perView: 2, spacing: 12 },
                },
                "(max-width: 1024px)": {
                    slides: { perView: 3, spacing: 14 },
                },
            },
            created: () => setLoaded(true),
            slideChanged: (slider) => setCurrentSlide(slider.track.details.rel),
        },
        [
            (slider) => {
                let timeout;
                let mouseOver = false;

                function clearNextTimeout() {
                    clearTimeout(timeout);
                }

                function nextTimeout() {
                    clearTimeout(timeout);
                    if (mouseOver) return;

                    if (slider?.track?.details) {
                        timeout = setTimeout(() => {
                            slider.next();
                        }, 3500);
                    }
                }

                slider.on("created", () => {
                    slider.container.addEventListener("mouseover", () => {
                        mouseOver = true;
                        clearNextTimeout();
                    });
                    slider.container.addEventListener("mouseout", () => {
                        mouseOver = false;
                        nextTimeout();
                    });
                    nextTimeout();
                });

                slider.on("dragStarted", clearNextTimeout);
                slider.on("animationEnded", nextTimeout);
                slider.on("updated", () => {
                    if (slider?.track?.details) nextTimeout();
                });
            },
        ]
    );

    return (
        <div className="relative max-w-6xl mx-auto px-4">
            <div ref={sliderRef} className="keen-slider">
                {books.map((book) => (
                    <div key={book.id} className="keen-slider__slide p-4">
                        <ProductCard product={book} />
                    </div>
                ))}
            </div>

            {loaded && instanceRef.current && (
                <>
                    <button
                        onClick={() => instanceRef.current?.prev()}
                        className="absolute -left-4 top-1/2 transform -translate-y-1/2 z-30
                                   bg-black border border-white/20 text-white p-2 rounded-full
                                   shadow-md hover:bg-[#D70018] transition"
                    >
                        <ChevronLeft className="w-4 h-4" />
                    </button>

                    <button
                        onClick={() => instanceRef.current?.next()}
                        className="absolute -right-4 top-1/2 transform -translate-y-1/2 z-30
                                   bg-black border border-white/20 text-white p-2 rounded-full
                                   shadow-md hover:bg-[#D70018] transition"
                    >
                        <ChevronRight className="w-4 h-4" />
                    </button>
                </>
            )}

            {loaded && instanceRef.current?.track?.details?.slides && (
                <div className="flex justify-center mt-6 gap-2">
                    {Array.from({
                        length: instanceRef.current.track.details.slides.length,
                    }).map((_, idx) => (
                        <button
                            key={idx}
                            onClick={() => instanceRef.current?.moveToIdx(idx)}
                            className={`w-3 h-3 rounded-full border border-white/30 transition ${
                                currentSlide === idx
                                    ? "bg-[#D70018]"
                                    : "bg-black"
                            }`}
                        />
                    ))}
                </div>
            )}
        </div>
    );
};

export default BookSlider;
