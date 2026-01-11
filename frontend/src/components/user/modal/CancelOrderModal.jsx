import { useState } from "react"
import { Dialog, Transition } from "@headlessui/react"
import { Fragment } from "react"
import { X, AlertTriangle, Calendar, CreditCard, ChevronLeft, ChevronRight } from "lucide-react"

const CANCELLATION_REASONS = [
    "Changed my mind",
    "Found a better price elsewhere",
    "Ordered by mistake",
    "No longer needed",
    "Delivery taking too long",
    "Other",
]

export function CancelOrderModal({ isOpen, order, onClose, onConfirm }) {
    const [reason, setReason] = useState("")
    const [customReason, setCustomReason] = useState("")
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState("")
    const [currentItemIndex, setCurrentItemIndex] = useState(0)

    const handleSubmit = async (e) => {
        e.preventDefault()

        if (!reason) {
            setError("Please select a reason for cancellation")
            return
        }

        if (reason === "Other" && !customReason.trim()) {
            setError("Please provide a custom reason")
            return
        }

        setLoading(true)
        setError("")

        try {
            const finalReason = reason === "Other" ? customReason : reason
            await onConfirm(order.id, finalReason)

            // Reset form
            setReason("")
            setCustomReason("")
        } catch (err) {
            setError("Failed to cancel order. Please try again.")
        } finally {
            setLoading(false)
        }
    }

    const handleClose = () => {
        setReason("")
        setCustomReason("")
        setError("")
        setCurrentItemIndex(0)
        onClose()
    }

    const formatDate = (dateString) =>
        new Date(dateString).toLocaleDateString("en-US", {
            year: "numeric",
            month: "short",
            day: "numeric",
        })

    const formatCurrency = (amount) => `$${amount.toFixed(2)}`

    const goToPreviousItem = () => {
        setCurrentItemIndex((prevIndex) => {
            if (prevIndex === 0) {
                return order.items.length - 1
            }
            return prevIndex - 1
        })
    }

    const goToNextItem = () => {
        setCurrentItemIndex((prevIndex) => {
            if (prevIndex === order.items.length - 1) {
                return 0
            }
            return prevIndex + 1
        })
    }

    if (!order) return null

    const currentItem = order.items[currentItemIndex] || {}
    const remainingItems = order.items.length - 1

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
                    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm" />
                </Transition.Child>

                <div className="fixed inset-0 overflow-y-auto">
                    <div className="flex min-h-full items-center justify-center p-4 text-center">
                        <Transition.Child
                            as={Fragment}
                            enter="ease-out duration-300"
                            enterFrom="opacity-0 scale-95"
                            enterTo="opacity-100 scale-100"
                            leave="ease-in duration-200"
                            leaveFrom="opacity-100 scale-100"
                            leaveTo="opacity-0 scale-95"
                        >
                            <Dialog.Panel className="w-full max-w-xl transform overflow-hidden rounded-2xl bg-white shadow-xl transition-all">
                                {/* Header */}
                                <div className="bg-red-500 px-6 py-4 text-white">
                                    <div className="flex items-center justify-between">
                                        <Dialog.Title className="text-xl font-bold flex items-center">
                                            <AlertTriangle className="mr-2" size={20} />
                                            Cancel Order
                                        </Dialog.Title>
                                        <button onClick={handleClose} className="text-white hover:text-white/80 transition-colors">
                                            <X size={20} />
                                        </button>
                                    </div>
                                    <p className="mt-1 text-sm text-white/90">
                                        We're sorry to see you go. Please help us understand why you're cancelling.
                                    </p>
                                </div>

                                <div className="p-5">
                                    {/* Order Summary */}
                                    <div className="mb-5 bg-gray-50 rounded-lg p-4 border border-gray-200">
                                        <h3 className="text-base font-semibold text-gray-900 mb-4 text-center">Order Summary</h3>

                                        <div className="grid grid-cols-2 gap-6">
                                            {/* Left side - Order Details */}
                                            <div className="space-y-3">
                                                <div className="flex items-center text-sm">
                                                    <span className="font-medium text-gray-600 w-20">Order ID:</span>
                                                    <span className="font-semibold text-gray-900">#{order.id.slice(-8)}</span>
                                                </div>
                                                <div className="flex items-center text-sm">
                                                    <Calendar className="mr-2 text-gray-500" size={16} />
                                                    <span className="font-medium text-gray-600 w-16">Date:</span>
                                                    <span className="text-gray-900">{formatDate(order.addedAt)}</span>
                                                </div>
                                                <div className="flex items-center text-sm">
                                                    <CreditCard className="mr-2 text-gray-500" size={16} />
                                                    <span className="font-medium text-gray-600 w-16">Total:</span>
                                                    <span className="font-semibold text-[#1C387F] text-lg">{formatCurrency(order.totalAmount)}</span>
                                                </div>
                                            </div>

                                            {/* Right side - Items */}
                                            <div>
                                                <p className="text-sm font-medium text-gray-600 mb-3 text-end">{order.items.length} Item{order.items.length > 1 && 's'}</p>
                                                {order.items.length > 0 && (
                                                    <div className="bg-white rounded-lg p-3 border border-gray-200">
                                                        <div className="flex items-center space-x-3">
                                                            <img
                                                                src={currentItem.imageUrl || "/placeholder.svg?height=40&width=30"}
                                                                alt={currentItem.title}
                                                                className="w-8 h-10 object-cover rounded border"
                                                            />
                                                            <div className="flex-1">
                                                                <p className="font-medium text-gray-900 text-sm">{currentItem.title}</p>
                                                                <p className="text-gray-500 text-xs">Qty: {currentItem.quantity}</p>
                                                            </div>
                                                        </div>
                                                        {order.items.length > 1 && (
                                                            <div className="mt-2 flex items-center justify-between">
                                                                <span className="text-xs text-gray-500">
                                                                  +{remainingItems} more {remainingItems === 1 ? "item" : "items"}
                                                                </span>
                                                                <div className="flex space-x-1">
                                                                    <button
                                                                        type="button"
                                                                        onClick={goToPreviousItem}
                                                                        className="w-6 h-6 rounded border border-gray-300 flex items-center justify-center text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
                                                                        aria-label="Previous item"
                                                                    >
                                                                        <ChevronLeft size={14} />
                                                                    </button>
                                                                    <button
                                                                        type="button"
                                                                        onClick={goToNextItem}
                                                                        className="w-6 h-6 rounded border border-gray-300 flex items-center justify-center text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
                                                                        aria-label="Next item"
                                                                    >
                                                                        <ChevronRight size={14} />
                                                                    </button>
                                                                </div>
                                                            </div>
                                                        )}
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    </div>

                                    {/* Warning Notice */}
                                    <div className="mb-5 p-2 bg-amber-50 border border-amber-200 rounded-lg text-center">
                                        <p className="text-xs text-amber-700">
                                            Once cancelled, this action cannot be undone. Any refunds will be processed according to our
                                            refund policy.
                                        </p>
                                    </div>

                                    <form onSubmit={handleSubmit} className="space-y-5">
                                        {/* Cancellation Reasons */}
                                        <div>
                                            <h4 className="text-base font-semibold text-gray-900 mb-3 text-center">
                                                Why are you cancelling this order?
                                            </h4>
                                            <div className="grid grid-cols-3 gap-3">
                                                {CANCELLATION_REASONS.map((reasonOption) => (
                                                    <label
                                                        key={reasonOption}
                                                        className={`flex items-center p-3 border rounded-lg cursor-pointer transition-all hover:bg-gray-50 ${
                                                            reason === reasonOption ? "border-[#1C387F] bg-blue-50" : "border-gray-200"
                                                        }`}
                                                    >
                                                        <input
                                                            type="radio"
                                                            name="reason"
                                                            value={reasonOption}
                                                            checked={reason === reasonOption}
                                                            onChange={(e) => setReason(e.target.value)}
                                                            className="mr-2 text-[#1C387F] focus:ring-[#1C387F]"
                                                        />
                                                        <span className={`text-sm ${reason === reasonOption ? "text-[#1C387F]" : "text-gray-700"}`}>
                              {reasonOption}
                            </span>
                                                    </label>
                                                ))}
                                            </div>
                                        </div>

                                        {/* Custom Reason */}
                                        {reason === "Other" && (
                                            <div>
                        <textarea
                            value={customReason}
                            onChange={(e) => setCustomReason(e.target.value)}
                            placeholder="Please provide your reason..."
                            rows={3}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-1 focus:ring-[#1C387F] focus:border-[#1C387F] resize-none text-sm placeholder:text-gray-400 placeholder:text-sm"
                        />
                                            </div>
                                        )}

                                        {/* Error Message */}
                                        {error && <p className="text-red-500 text-xs text-center">{error}</p>}

                                        {/* Action Buttons */}
                                        <div className="flex space-x-3 pt-2">
                                            <button
                                                type="button"
                                                onClick={handleClose}
                                                className="flex-1 px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors text-sm font-medium"
                                            >
                                                Keep Order
                                            </button>
                                            <button
                                                type="submit"
                                                disabled={loading || !reason}
                                                className="flex-1 px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                                            >
                                                {loading ? "Processing..." : "Cancel Order"}
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
