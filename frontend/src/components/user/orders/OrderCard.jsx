import { Package, Truck, CheckCircle, XCircle, Clock, Calendar, CreditCard, MapPin, Star, Ban } from "lucide-react"

export function OrderCard({ order, onWriteReview, onCancelOrder }) {
    const getOrderStatusIcon = (status) => {
        switch (status) {
            case "PENDING":
            case "PENDING_REFUND":
                return <Clock className="text-yellow-600" size={18} />
            case "PROCESSING":
                return <Package className="text-blue-600" size={18} />
            case "SHIPPED":
                return <Truck className="text-purple-600" size={18} />
            case "DELIVERED":
            case "COMPLETED":
                return <CheckCircle className="text-green-600" size={18} />
            case "CANCELLED":
            case "REFUNDED":
                return <XCircle className="text-red-600" size={18} />
            default:
                return <Clock className="text-gray-600" size={18} />
        }
    }

    const getOrderStatusColor = (status) => {
        switch (status) {
            case "PENDING":
            case "PENDING_REFUND":
                return "bg-gradient-to-r from-yellow-50 to-yellow-100 text-yellow-800 border-yellow-200 shadow-yellow-100"
            case "PROCESSING":
                return "bg-gradient-to-r from-blue-50 to-blue-100 text-blue-800 border-blue-200 shadow-blue-100"
            case "SHIPPED":
                return "bg-gradient-to-r from-purple-50 to-purple-100 text-purple-800 border-purple-200 shadow-purple-100"
            case "DELIVERED":
            case "COMPLETED":
                return "bg-gradient-to-r from-green-50 to-green-100 text-green-800 border-green-200 shadow-green-100"
            case "CANCELLED":
            case "REFUNDED":
                return "bg-gradient-to-r from-red-50 to-red-100 text-red-800 border-red-200 shadow-red-100"
            default:
                return "bg-gradient-to-r from-gray-50 to-gray-100 text-gray-800 border-gray-200 shadow-gray-100"
        }
    }

    const formatDate = (dateString) =>
        new Date(dateString).toLocaleDateString("en-US", {
            year: "numeric",
            month: "short",
            day: "numeric",
        })

    const formatCurrency = (amount) => `$${amount.toFixed(2)}`

    const canWriteReview = (status, hasReviewed) => (status === "DELIVERED" || status === "COMPLETED") && !hasReviewed
    const canCancelOrder = (status) => status === "PENDING" || status === "PROCESSING"

    return (
        <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-lg hover:shadow-xl transition-all duration-300 hover:border-gray-300 group">
            {/* Header Section */}
            <div className="flex items-start justify-between mb-6">
                <div className="flex items-center space-x-4">
                    <div className="p-2 bg-gray-50 rounded-xl group-hover:bg-gray-100 transition-colors">
                        {getOrderStatusIcon(order.orderStatus)}
                    </div>
                    <div>
            <span
                className={`inline-flex items-center px-4 py-2 rounded-xl text-sm font-semibold border shadow-sm ${getOrderStatusColor(
                    order.orderStatus,
                )}`}
            >
              {order.orderStatus === "PENDING_REFUND" ? "PENDING REFUND" : order.orderStatus}
            </span>
                    </div>
                </div>

                <div className="text-right space-y-1">
                    <p className="text-sm font-semibold text-gray-700">Order #{order.id.slice(-8)}</p>
                    <p className="text-xs text-gray-500 flex items-center justify-end">
                        <Calendar size={12} className="mr-1" />
                        {formatDate(order.addedAt)}
                    </p>
                </div>
            </div>

            <div className="grid lg:grid-cols-3 gap-8">
                {/* Items Section */}
                <div className="lg:col-span-2">
                    <div className="flex items-center justify-between mb-4">
                        <h4 className="text-lg font-bold text-gray-900">Order Items</h4>
                        <span className="text-sm text-gray-500 bg-gray-100 px-3 py-1 rounded-full">
              {order.items.length} {order.items.length === 1 ? "item" : "items"}
            </span>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        {order.items.map((item, index) => (
                            <div
                                key={index}
                                className="flex items-center space-x-4 p-3 bg-gradient-to-r from-gray-50 to-gray-100 rounded-xl hover:from-gray-100 hover:to-gray-200 transition-all duration-200 border border-gray-200"
                            >
                                <div className="relative">
                                    <img
                                        src={item.imageUrl || "/placeholder.svg?height=60&width=45"}
                                        alt={item.title}
                                        className="w-12 h-16 object-cover rounded-lg border-2 border-white shadow-sm"
                                    />
                                    <div className="absolute -top-2 -right-2 bg-[#1C387F] text-white text-xs rounded-full w-5 h-5 flex items-center justify-center font-semibold">
                                        {item.quantity}
                                    </div>
                                </div>
                                <div className="flex-1">
                                    <h5 className="font-semibold text-gray-900 text-sm">{item.title}</h5>
                                    <p className="text-xs text-gray-600">Qty: {item.quantity}</p>
                                    <p className="text-sm font-bold text-[#1C387F]">{formatCurrency(item.price)}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Details Section */}
                <div className="space-y-6">
                    {/* Payment Card */}
                    <div className="bg-gradient-to-br from-purple-50 to-violet-50 p-5 rounded-xl border border-purple-200">
                        <h4 className="font-bold text-gray-900 mb-3 flex items-center">
                            <div className="p-2 bg-purple-100 rounded-lg mr-3">
                                <CreditCard size={18} className="text-purple-600" />
                            </div>
                            Payment Details
                        </h4>
                        <div className="space-y-2">
                            <p className="text-sm text-gray-700 capitalize">
                                <span className="font-medium">Method:</span> {order.payment.method.replace("_", " ")}
                            </p>
                            <p className="text-2xl font-bold text-[#1C387F]">{formatCurrency(order.totalAmount)}</p>
                            {order.payment.transactions.length > 0 && (
                                <p className="text-xs text-gray-600 bg-white px-2 py-1 rounded-md inline-block">
                                    Status: {order.payment.transactions[order.payment.transactions.length - 1].status}
                                </p>
                            )}
                        </div>
                    </div>

                    {/* Shipping Card */}
                    <div className="bg-gradient-to-br from-orange-50 to-amber-50 p-5 rounded-xl border border-orange-200">
                        <h4 className="font-bold text-gray-900 mb-3 flex items-center">
                            <div className="p-2 bg-orange-100 rounded-lg mr-3">
                                <MapPin size={18} className="text-orange-600" />
                            </div>
                            Shipping Address
                        </h4>
                        <div className="space-y-1 text-sm">
                            <p className="font-semibold text-gray-800">
                                {order.shippingInformation.firstName} {order.shippingInformation.lastName}
                            </p>
                            <p className="text-gray-600">{order.shippingInformation.address.street}</p>
                            <p className="text-gray-600">
                                {order.shippingInformation.address.city}, {order.shippingInformation.address.state}{" "}
                                {order.shippingInformation.address.postalCode}
                            </p>
                            <p className="text-gray-600">{order.shippingInformation.address.country}</p>
                        </div>
                    </div>

                    {/* Action Buttons */}
                    <div className="space-y-3">
                        {canWriteReview(order.orderStatus, order.reviewed) && (
                            <button
                                onClick={() => onWriteReview(order)}
                                className="w-full flex items-center justify-center space-x-3 bg-gradient-to-r from-[#1C387F] to-[#2A4A8F] hover:from-[#153066] hover:to-[#1F3A75] text-white py-3 px-4 rounded-xl transition-all duration-200 font-semibold shadow-lg hover:shadow-xl transform hover:-translate-y-0.5"
                            >
                                <Star size={18} />
                                <span>Write Review</span>
                            </button>
                        )}

                        {canCancelOrder(order.orderStatus) && (
                            <button
                                onClick={() => onCancelOrder(order)}
                                className="w-full flex items-center justify-center space-x-3 bg-gradient-to-r from-red-500 to-red-600 hover:from-red-600 hover:to-red-700 text-white py-3 px-4 rounded-xl transition-all duration-200 font-semibold shadow-lg hover:shadow-xl transform hover:-translate-y-0.5"
                            >
                                <Ban size={18} />
                                <span>Cancel Order</span>
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    )
}
