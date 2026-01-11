import {useState, Fragment, useEffect} from "react"
import {Dialog, Transition} from "@headlessui/react"
import {X, Star} from "lucide-react"
import RatingStar from "@u_components/products/RatingStar.jsx"

function DelayedRating({ratingValue, onChange}) {
    const [ready, setReady] = useState(false);
    useEffect(() => {
        const id = setTimeout(() => setReady(true), 0);
        return () => clearTimeout(id);
    }, []);
    if (!ready) return null;
    return <RatingStar ratingValue={ratingValue} onChange={onChange}/>;
}

export function ReviewModal({isOpen, order, onClose, onSubmit}) {
    const [formData, setFormData] = useState({
        reviews:
            order?.items?.map((item) => ({
                bookId: item.bookId,
                rating: 5, // Default
                subject: "",
                comment: "",
            })) || [],
    })
    const [loading, setLoading] = useState(false)
    const [errors, setErrors] = useState({})

    const handleSubmit = async (e) => {
        e.preventDefault()
        setLoading(true)
        setErrors({})
        try {
            await onSubmit({
                orderId: order.id,
                reviews: formData.reviews.map((r) => ({
                    bookId: r.bookId,
                    rating: r.rating,
                    subject: r.subject,
                    comment: r.comment,
                })),
            })
            // Reset form
            setFormData({
                reviews: order.items.map((item) => ({
                    bookId: item.bookId,
                    rating: 5,
                    subject: "",
                    comment: "",
                })),
            })
        } catch {
            setErrors({submit: "Failed to submit reviews. Please try again."})
        } finally {
            setLoading(false)
        }
    }

    const handleClose = () => {
        setFormData({
            reviews:
                order?.items?.map((item) => ({
                    bookId: item.bookId,
                    rating: 5,
                    subject: "",
                    comment: "",
                })) || [],
        })
        setErrors({})
        onClose()
    }

    if (!order) return null

    return (
        <Transition appear show={isOpen} as={Fragment}>
            <Dialog as="div" className="relative z-50" onClose={handleClose}>
                <Transition.Child
                    as={Fragment}
                    enter="ease-out duration-300"
                    enterFrom="opacity-0"
                    enterTo="opacity-100"
                    leave="ease-in duration-200"
                    leaveFrom="opacity-100"
                    leaveTo="opacity-0"
                >
                    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm"/>
                </Transition.Child>

                <div className="fixed inset-0 overflow-y-auto">
                    <div className="flex min-h-full items-center justify-center p-4">
                        <Transition.Child
                            as={Fragment}
                            enter="ease-out duration-300"
                            enterFrom="opacity-0 scale-95"
                            enterTo="opacity-100 scale-100"
                            leave="ease-in duration-200"
                            leaveFrom="opacity-100 scale-100"
                            leaveTo="opacity-0 scale-95"
                        >
                            <Dialog.Panel
                                className="w-full max-w-4xl transform overflow-hidden rounded-3xl bg-white shadow-2xl transition-all">
                                {/* Header */}
                                <div className="bg-gradient-to-r from-[#1C387F] to-[#2A4A8F] px-8 py-6 text-white">
                                    <div className="flex items-center justify-between">
                                        <Dialog.Title className="text-xl font-bold flex items-center">
                                            <div className="p-2 bg-white/20 rounded-xl mr-3">
                                                <Star size={20}/>
                                            </div>
                                            Write a Review
                                        </Dialog.Title>
                                        <button
                                            onClick={handleClose}
                                            className="p-2 hover:bg-white/20 rounded-xl transition-colors"
                                        >
                                            <X size={24}/>
                                        </button>
                                    </div>
                                    <p className="mt-1 text-sm text-blue-100">
                                        Share your experience and help other readers discover
                                        great books
                                    </p>
                                </div>

                                <div className="p-8">
                                    <form onSubmit={handleSubmit} className="space-y-8">
                                        {/* Individual Book Reviews */}
                                        {formData.reviews.map((review, index) => {
                                            const item = order.items[index]
                                            return (
                                                <div
                                                    key={item.bookId}
                                                    className="bg-white border-2 border-gray-200 rounded-2xl p-6 shadow-sm"
                                                >
                                                    {/* Book Header */}
                                                    <div className="flex items-start space-x-6 mb-8">
                                                        <img
                                                            src={
                                                                item.imageUrl ||
                                                                "/placeholder.svg?height=80&width=60"
                                                            }
                                                            alt={item.title}
                                                            className="w-16 h-20 object-cover rounded-lg border-2 border-gray-200 shadow-sm flex-shrink-0"
                                                        />
                                                        <div>
                                                            <h3 className="text-xl font-bold text-gray-900 mb-2">
                                                                {item.title}
                                                            </h3>
                                                            <p className="text-sm text-gray-600 mb-4">
                                                                Quantity: {item.quantity}
                                                            </p>
                                                        </div>
                                                        {/* Rating Section */}
                                                        <div className="flex items-center ml-auto space-x-4">
                                                            <DelayedRating
                                                                ratingValue={review.rating}
                                                                onChange={(value) => {
                                                                    const newReviews = [...formData.reviews]
                                                                    newReviews[index] = {
                                                                        ...newReviews[index],
                                                                        rating: value
                                                                    }
                                                                    setFormData((prev) => ({
                                                                        ...prev,
                                                                        reviews: newReviews
                                                                    }))
                                                                }}
                                                            />
                                                            <div className="flex items-center space-x-2">
                                                                {review.rating > 0 && (
                                                                    <span
                                                                        className="text-sm font-medium text-gray-700 bg-gray-100 px-2 py-1 rounded-full">
                                                                        {review.rating === 5 && "Excellent! 🌟"}
                                                                        {review.rating === 4 && "Very Good! 👍"}
                                                                        {review.rating === 3 && "Good 👌"}
                                                                        {review.rating === 2 && "Fair 😐"}
                                                                        {review.rating === 1 && "Poor 👎"}
                                                                    </span>
                                                                )}
                                                            </div>
                                                        </div>


                                                    </div>

                                                    {/* Review Form */}
                                                    <div className="space-y-6">
                                                        {/* Review Title */}
                                                        <div>
                                                            <label
                                                                className="block text-sm font-semibold text-gray-900 mb-2">
                                                                Review Title
                                                            </label>
                                                            <input
                                                                type="text"
                                                                value={review.subject}
                                                                onChange={(e) => {
                                                                    const newReviews = [
                                                                        ...formData.reviews,
                                                                    ]
                                                                    newReviews[index] = {
                                                                        ...newReviews[index],
                                                                        subject: e.target.value,
                                                                    }
                                                                    setFormData((prev) => ({
                                                                        ...prev,
                                                                        reviews: newReviews,
                                                                    }))
                                                                }}
                                                                placeholder="Give your review a catchy title"
                                                                className="
                                                                  w-full px-4 py-3 border border-gray-300 rounded-lg
                                                                  placeholder:text-sm placeholder:text-gray-400
                                                                  focus:ring-2 focus:ring-[#1C387F] focus:border-[#1C387F]
                                                                  transition-colors
                                                                "
                                                            />
                                                        </div>

                                                        {/* Review Content */}
                                                        <div>
                                                            <label
                                                                className="block text-sm font-semibold text-gray-900 mb-2">
                                                                Your Review
                                                            </label>
                                                            <textarea
                                                                value={review.comment}
                                                                onChange={(e) => {
                                                                    const newReviews = [
                                                                        ...formData.reviews,
                                                                    ]
                                                                    newReviews[index] = {
                                                                        ...newReviews[index],
                                                                        comment: e.target.value,
                                                                    }
                                                                    setFormData((prev) => ({
                                                                        ...prev,
                                                                        reviews: newReviews,
                                                                    }))
                                                                }}
                                                                placeholder="What did you think about this book? Share your thoughts, favorite parts, or what others should know..."
                                                                rows={4}
                                                                className="
                                                                  w-full px-4 py-3 border border-gray-300 rounded-lg
                                                                  placeholder:text-sm placeholder:text-gray-400
                                                                  focus:ring-2 focus:ring-[#1C387F] focus:border-[#1C387F]
                                                                  resize-none transition-colors
                                                                "
                                                            />
                                                        </div>
                                                    </div>
                                                </div>
                                            )
                                        })}

                                        {/* Submit Error */}
                                        {errors.submit && (
                                            <div className="p-4 bg-red-50 border border-red-200 rounded-xl">
                                                <p className="text-red-700 font-medium">
                                                    {errors.submit}
                                                </p>
                                            </div>
                                        )}

                                        {/* Action Buttons */}
                                        <div className="flex space-x-4 pt-4">
                                            <button
                                                type="button"
                                                onClick={handleClose}
                                                className="flex-1 px-6 py-3 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-all duration-200 font-medium"
                                            >
                                                Cancel
                                            </button>
                                            <button
                                                type="submit"
                                                disabled={loading}
                                                className="flex-1 px-6 py-3 bg-gradient-to-r from-[#1C387F] to-[#2A4A8F] hover:from-[#153066] hover:to-[#1F3A75] text-white rounded-lg transition-all duration-200 font-medium shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
                                            >
                                                {loading ? (
                                                    <span className="flex items-center justify-center">
                                                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"/>
                                                    Submitting Reviews...
                                                  </span>
                                                ) : (
                                                    `Submit ${formData.reviews.length} Review${
                                                        formData.reviews.length !== 1 ? "s" : ""
                                                    }`
                                                )}
                                            </button>
                                        </div>
                                    </form>
                                </div>
                            </Dialog.Panel>
                        </Transition.Child>
                    </div>
                </div>
            </Dialog>
        </Transition>
    )
}
