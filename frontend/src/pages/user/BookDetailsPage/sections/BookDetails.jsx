import { useContext, useRef, useState } from "react"
import { Minus, Plus, ChevronLeft, ChevronRight } from "lucide-react"
import WishlistBtn from "@u_components/products/WishlistBtn.jsx"
import RatingStar from "@u_components/products/RatingStar.jsx"
import ConditionTag from "@u_components/products/ConditionTag.jsx"
import { useCartActions } from "@u_hooks/useCartActions.js"
import { showError } from "@utils/toast.js"
import { useNavigate } from "react-router-dom"
import { CheckoutContext } from "@contexts/CheckoutContext.jsx"

export default function BookDetails({ bookDetails }) {
    const {
        id,
        title,
        authors,
        stock,
        condition,
        averageRating,
        ratingCount,
        categoryNames,
        price,
        images = [],
        description = "",
    } = bookDetails || {}

    const { addToCart } = useCartActions()
    const navigate = useNavigate()
    const { setSelectedItems } = useContext(CheckoutContext)

    const [quantity, setQuantity] = useState(1)
    const [isImageExpanded, setIsImageExpanded] = useState(false)
    const [currentImageIndex, setCurrentImageIndex] = useState(0)

    const mainImage = images[currentImageIndex] || { url: "/book-placeholder.jpg", alt: title }
    const descRef = useRef(null)

    const authorName = authors?.join(", ") || ""
    const genres = categoryNames?.join(", ") || ""
    const hasRating = ratingCount > 0
    const isOOS = stock < 1
    const firstAuthor = authorName.split(",")?.[0]?.trim()

    const handleAddToCart = async () => {
        if (isOOS) {
            showError(`"${title}" is out of stock!`)
            return
        }
        await addToCart({
            bookId: id,
            title,
            author: firstAuthor,
            price,
            quantity,
            image: images?.[0]?.url,
            condition,
            stock,
        })
    }

    const handleBuyNow = async () => {
        await handleAddToCart()
        setSelectedItems([{
            bookId: id,
            title,
            author: authorName,
            price,
            quantity: 1,
            image: images?.[0]?.url,
            condition,
            stock,
        }])
        navigate("/checkout")
    }

    return (
        <div className="max-w-6xl mx-auto p-4">
            <div className="grid grid-cols-1 md:grid-cols-12 gap-12">
                <div className="md:col-span-4">
                    <div className="group border border-gray-200 rounded-lg bg-white p-4 relative">
                        {images.length > 1 && (
                            <button
                                onClick={() =>
                                    setCurrentImageIndex(i => i > 0 ? i - 1 : images.length - 1)
                                }
                                className="hidden group-hover:flex absolute left-2 top-1/2 -translate-y-1/2
                                           bg-white border rounded-full p-1 shadow z-10"
                            >
                                <ChevronLeft className="w-5 h-5" />
                            </button>
                        )}

                        <div className="absolute top-3 left-3 z-10">
                            <ConditionTag type={condition} />
                        </div>

                        <div
                            className="w-full aspect-square flex items-center justify-center
                                       bg-white rounded-md p-4 cursor-zoom-in"
                            onClick={() => setIsImageExpanded(true)}
                        >
                            <img
                                src={mainImage.url}
                                alt={mainImage.alt}
                                className="w-full h-full object-contain"
                            />
                        </div>

                        <WishlistBtn bookId={id} />

                        {images.length > 1 && (
                            <button
                                onClick={() =>
                                    setCurrentImageIndex(i => i < images.length - 1 ? i + 1 : 0)
                                }
                                className="hidden group-hover:flex absolute right-2 top-1/2 -translate-y-1/2
                                           bg-white border rounded-full p-1 shadow z-10"
                            >
                                <ChevronRight className="w-5 h-5" />
                            </button>
                        )}
                    </div>
                </div>

                <div className="md:col-span-7 space-y-5">
                    <div className="flex items-center gap-4">
                        <h1 className="text-3xl font-semibold text-black">{title}</h1>
                        {!isOOS && (
                            <span className="bg-red-100 text-[#D70018] text-xs font-semibold px-3 py-1 rounded">
                                {stock} items
                            </span>
                        )}
                    </div>

                    <div className="flex items-center gap-4">
                        <span className="text-gray-600">{authorName}</span>
                        <span className="bg-red-50 text-[#D70018] text-[10px] font-semibold px-3 py-1 rounded">
                            {genres}
                        </span>
                    </div>

                    {hasRating && (
                        <div className="flex items-center">
                            <RatingStar ratingValue={averageRating} />
                            <span className="ml-2 text-sm text-gray-500">
                                ({ratingCount} reviews)
                            </span>
                        </div>
                    )}

                    <p className="text-gray-700">
                        {description.slice(0, 150)}...
                        <button
                            className="ml-1 text-[#D70018] hover:underline font-medium"
                            onClick={() => descRef.current?.scrollIntoView({ behavior: "smooth" })}
                        >
                            See more
                        </button>
                    </p>

                    <div className="text-3xl font-bold text-[#D70018]">
                        ${price?.toFixed(2)}
                    </div>

                    <div className="flex items-center gap-6">
                        <div className="flex items-center border border-gray-300 rounded-md">
                            <button
                                onClick={() => setQuantity(q => Math.max(1, q - 1))}
                                className="px-4 py-3 text-gray-600"
                            >
                                <Minus className="w-4 h-4" />
                            </button>
                            <span className="px-4">{quantity}</span>
                            <button
                                onClick={() => setQuantity(q => Math.min(stock, q + 1))}
                                className="px-4 py-3 text-gray-600"
                            >
                                <Plus className="w-4 h-4" />
                            </button>
                        </div>

                        <button
                            onClick={handleBuyNow}
                            disabled={isOOS}
                            className="bg-[#D70018] hover:bg-[#B80012] text-white
                                       px-8 h-[40px] rounded-[10px] font-semibold transition"
                        >
                            BUY NOW
                        </button>
                    </div>

                    <button
                        onClick={handleAddToCart}
                        disabled={isOOS}
                        className="mt-4 border-2 border-[#D70018] text-[#D70018]
                                   hover:bg-[#D70018] hover:text-white
                                   w-full max-w-[320px] py-2 rounded-[10px] font-semibold transition"
                    >
                        ADD TO CART
                    </button>
                </div>
            </div>

            <div className="mt-16" ref={descRef}>
                <h2 className="text-xl font-semibold text-[#D70018] mb-4">Description</h2>
                <div className="bg-gray-100 border border-gray-200 rounded-md p-6">
                    {description}
                </div>
            </div>

            {isImageExpanded && (
                <div
                    className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center"
                    onClick={() => setIsImageExpanded(false)}
                >
                    <img
                        src={mainImage.url}
                        alt={mainImage.alt}
                        className="max-w-[90vw] max-h-[90vh]"
                    />
                </div>
            )}
        </div>
    )
}
