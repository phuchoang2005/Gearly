import { useKeenSlider } from "keen-slider/react"
import { Layers } from "lucide-react"
import { useState } from "react"
import { Link } from "react-router-dom"

const getCategoryColor = (index) => {
    const colors = [
        "#D70018",
        "#B80012",
        "#90000F",
        "#1A1A1A",
        "#2D2D2D",
        "#3A3A3A",
        "#0EA5E9",
        "#8B5CF6",
        "#22C55E",
        "#F97316",
        "#EF4444",
        "#DC2626",
        "#7C2D12",
        "#991B1B",
        "#450A0A",
        "#111827",
        "#1F2937",
        "#374151",
        "#4B5563",
        "#6B7280",
    ]

    return colors[index % colors.length]
}

const animation = { duration: 15000, easing: (t) => t }

const CategorySlider = ({ categories }) => {
    const [loaded, setLoaded] = useState(false)

    const [sliderRef] = useKeenSlider({
        loop: true,
        renderMode: "performance",
        slides: {
            perView: 5,
            spacing: 24,
        },
        breakpoints: {
            "(max-width: 640px)": {
                slides: { perView: 1.2, spacing: 16 },
            },
            "(max-width: 768px)": {
                slides: { perView: 2.2, spacing: 16 },
            },
            "(max-width: 1024px)": {
                slides: { perView: 3.2, spacing: 20 },
            },
            "(max-width: 1280px)": {
                slides: { perView: 4, spacing: 20 },
            },
        },
        created(slider) {
            setLoaded(true)
            slider.moveToIdx(5, true, animation)
        },
        updated(slider) {
            slider.moveToIdx(slider.track.details.abs + 5, true, animation)
        },
        animationEnded(slider) {
            slider.moveToIdx(slider.track.details.abs + 5, true, animation)
        },
    })

    return (
        <div className="max-w-6xl mx-auto px-6 py-18">
            <div className="text-center mb-10">
                <h2
                    className="inline-flex items-center gap-3 px-8 py-3 border-2 border-transparent rounded-full
                    text-white font-bold uppercase tracking-wide text-xl
                    bg-gradient-to-r from-[#D70018] to-[#B80012]
                    shadow-lg hover:from-[#B80012] hover:to-[#D70018]
                    transition duration-300 ease-in-out transform hover:scale-105 cursor-pointer"
                >
                    <Layers className="w-6 h-6" />
                    Browse PC Categories
                </h2>

                <p className="text-gray-600 max-w-2xl mx-auto mt-4">
                    Explore PC components by category to build or upgrade your setup.
                </p>
            </div>

            <div className="relative">
                <div ref={sliderRef} className="keen-slider">
                    {categories.map((category, idx) => (
                        <Link
                            key={category.id}
                            to={`/shop?genres=${encodeURIComponent(category.id)}`}
                            className="keen-slider__slide px-1 py-2"
                        >
                            <div
                                className="rounded-xl overflow-hidden h-72 relative cursor-pointer shadow-md
                                           transform transition-all duration-300 ease-in-out hover:scale-105"
                                style={{ backgroundColor: getCategoryColor(idx) }}
                            >
                                <div className="absolute top-6 left-6 text-white/90">
                                    <Layers className="w-8 h-8" />
                                </div>

                                <div className="absolute top-6 right-6">
                                    <span className="px-4 py-1.5 bg-white/20 backdrop-blur-sm rounded-full text-white text-sm font-medium">
                                        {category.bookCount}{" "}
                                        {category.bookCount === 1 ? "item" : "items"}
                                    </span>
                                </div>

                                <div className="absolute bottom-6 left-6 right-6">
                                    <h3 className="text-white text-2xl font-bold">
                                        {category.name}
                                    </h3>
                                </div>
                            </div>
                        </Link>
                    ))}
                </div>
            </div>
        </div>
    )
}

export default CategorySlider
