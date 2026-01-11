import {useContext, useEffect} from 'react'
import {useLocation, useNavigate, Link} from 'react-router-dom'
import CheckoutProgress from '@u_components/checkout/CheckoutProgress.jsx'
import {CheckCircle, Package, CreditCard, ShoppingBag, MapPin} from 'lucide-react'
import {CheckoutContext} from '@contexts/CheckoutContext.jsx'

const formatDate = (dateString) => {
    if (!dateString) return 'N/A'
    try {
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        })
    } catch {
        return dateString
    }
}

const formatCurrency = (amount) => {
    if (typeof amount !== 'number') return 'N/A'
    return amount.toLocaleString('en-US', {style: 'currency', currency: 'USD'})
}

const statusTagColors = {
    PENDING: {text: 'text-orange-700', bgColor: 'rgba(251,191,36,0.15)'},
    PROCESSING: {text: 'text-blue-700', bgColor: 'rgba(59,130,246,0.15)'},
    SUCCESSFUL: {text: 'text-green-700', bgColor: 'rgba(34,197,94,0.15)'},
    FAILED: {text: 'text-red-700', bgColor: 'rgba(239,68,68,0.15)'},
    UNKNOWN: {text: 'text-gray-700', bgColor: 'rgba(107,114,128,0.15)'},
}

export default function OrderConfirmationPage() {
    const location = useLocation()
    const navigate = useNavigate()

    const {
        setSelectedItems,
        shippingAddress: flatShippingAddress,
        setShippingAddress,
        setUsingSaved,
        setOrderCompleted,
    } = useContext(CheckoutContext)

    const {orderDetails, paymentMethod} = location.state || {}

    useEffect(() => {
        setSelectedItems([])
        setUsingSaved(false)
        setOrderCompleted(true)

        if (!orderDetails?.id || !flatShippingAddress) {
            navigate('/', {replace: true})
            return
        }

        return () => setShippingAddress(null)
    }, [flatShippingAddress, orderDetails, navigate])

    if (!orderDetails?.id || !flatShippingAddress) {
        return (
            <div className="flex justify-center items-center min-h-screen bg-neutral-100">
                <p className="text-lg text-gray-700">Loading order details…</p>
            </div>
        )
    }

    const itemsSubtotal = orderDetails.items.reduce(
        (sum, item) => sum + item.price * item.quantity,
        0
    )
    const shippingAndTaxes = orderDetails.totalAmount - itemsSubtotal

    const transactions = orderDetails.payment?.transactions || []
    const latestTransaction = [...transactions].sort(
        (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
    )[0]

    const paymentStatus = latestTransaction?.status || 'UNKNOWN'
    const paymentColor = statusTagColors[paymentStatus] || statusTagColors.UNKNOWN

    const orderStatus = orderDetails.orderStatus || 'UNKNOWN'
    const orderColor = statusTagColors[orderStatus] || statusTagColors.UNKNOWN

    return (
        <div className="bg-neutral-100 min-h-screen py-16">
            <div className="max-w-screen-lg mx-auto px-4">
                <CheckoutProgress step="confirmation"/>

                <div className="bg-white rounded-xl shadow-2xl p-8 md:p-12 mt-10">
                    {/* Header */}
                    <div className="text-center border-b pb-8 mb-10">
                        <CheckCircle className="w-20 h-20 text-[#D70018] mx-auto mb-5"/>
                        <h1 className="text-3xl font-bold text-black">
                            Order Confirmed
                        </h1>
                        <p className="text-gray-600 mt-2">
                            Thank you for shopping with Gearly.
                        </p>
                        <p className="text-lg font-semibold mt-4">
                            Order ID:{' '}
                            <span className="text-[#D70018]">{orderDetails.id}</span>
                        </p>
                        <p className="text-sm text-gray-500 mt-1">
                            Order Date: {formatDate(orderDetails.addedAt)}
                        </p>
                    </div>

                    <div className="grid lg:grid-cols-7 gap-10">
                        {/* Order summary */}
                        <div className="lg:col-span-4">
                            <div className="flex justify-between items-center mb-6">
                                <h2 className="text-xl font-semibold flex items-center text-black">
                                    <Package className="mr-3 text-[#D70018]"/> Order Summary
                                </h2>
                                <span
                                    className={`px-4 py-1 rounded-full text-sm font-semibold ${orderColor.text}`}
                                    style={{backgroundColor: orderColor.bgColor}}
                                >
                                    {orderStatus}
                                </span>
                            </div>

                            <div className="space-y-4 max-h-[28rem] overflow-y-auto pr-2">
                                {orderDetails.items.map((item, idx) => (
                                    <div
                                        key={item.bookId || idx}
                                        className="flex justify-between items-start bg-neutral-50 p-4 rounded-md border"
                                    >
                                        <div className="flex">
                                            <img
                                                src={item.imageUrl}
                                                alt={item.title}
                                                className="w-16 h-20 object-cover rounded mr-4 border"
                                            />
                                            <div>
                                                <p className="font-semibold text-sm">
                                                    {item.title}
                                                </p>
                                                <p className="text-xs text-gray-500 mt-1">
                                                    Qty: {item.quantity} × {formatCurrency(item.price)}
                                                </p>
                                            </div>
                                        </div>
                                        <p className="font-semibold text-sm">
                                            {formatCurrency(item.price * item.quantity)}
                                        </p>
                                    </div>
                                ))}
                            </div>

                            <div className="border-t pt-6 mt-6 space-y-2 text-sm">
                                <div className="flex justify-between">
                                    <span>Subtotal</span>
                                    <span>{formatCurrency(itemsSubtotal)}</span>
                                </div>
                                <div className="flex justify-between">
                                    <span>Shipping & Taxes</span>
                                    <span>{formatCurrency(shippingAndTaxes)}</span>
                                </div>
                                <div className="flex justify-between font-bold text-lg text-[#D70018] pt-3 border-t">
                                    <span>Total</span>
                                    <span>{formatCurrency(orderDetails.totalAmount)}</span>
                                </div>
                            </div>
                        </div>

                        {/* Right column */}
                        <div className="lg:col-span-3 space-y-8">
                            {/* Address */}
                            <div>
                                <h2 className="text-xl font-semibold mb-4 flex items-center text-black">
                                    <MapPin className="mr-3 text-[#D70018]"/> Shipping Address
                                </h2>
                                <div className="bg-neutral-50 border rounded-md p-5 text-sm">
                                    <p className="font-semibold">
                                        {flatShippingAddress.firstName} {flatShippingAddress.lastName}
                                    </p>
                                    <p>{flatShippingAddress.street}</p>
                                    <p>
                                        {flatShippingAddress.city}, {flatShippingAddress.state}{' '}
                                        {flatShippingAddress.postalCode}
                                    </p>
                                    <p>{flatShippingAddress.country}</p>
                                    <div className="border-t mt-3 pt-3 text-xs text-gray-600">
                                        <p>Email: {flatShippingAddress.email}</p>
                                        <p>Phone: {flatShippingAddress.phoneNumber}</p>
                                    </div>
                                </div>
                            </div>

                            {/* Payment */}
                            <div>
                                <h2 className="text-xl font-semibold mb-4 flex items-center text-black">
                                    <CreditCard className="mr-3 text-[#D70018]"/> Payment
                                </h2>
                                <div className="bg-neutral-50 border rounded-md p-5 text-sm">
                                    <p>
                                        Method:{' '}
                                        <span className="font-semibold">
                                            {paymentMethod?.toUpperCase()}
                                        </span>
                                    </p>
                                    {latestTransaction && (
                                        <p className="mt-2">
                                            Status:{' '}
                                            <span
                                                className={`px-3 py-1 rounded-full font-semibold ${paymentColor.text}`}
                                                style={{backgroundColor: paymentColor.bgColor}}
                                            >
                                                {paymentStatus}
                                            </span>
                                        </p>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Actions */}
                    <div className="border-t pt-10 mt-12 text-center">
                        <p className="text-sm text-gray-600 mb-8">
                            Confirmation email sent to{' '}
                            <span className="font-semibold text-[#D70018]">
                                {flatShippingAddress.email}
                            </span>
                        </p>

                        <div className="flex flex-col sm:flex-row justify-center gap-4">
                            <Link
                                to="/shop"
                                replace
                                className="flex items-center justify-center gap-2 px-10 py-3 bg-[#D70018] text-white rounded-lg font-semibold hover:bg-[#B80012] transition shadow"
                            >
                                <ShoppingBag size={20}/> Continue Shopping
                            </Link>

                            <Link
                                to="/me/orders"
                                replace
                                className="px-10 py-3 border-2 border-black text-black rounded-lg font-semibold hover:bg-black hover:text-white transition shadow"
                            >
                                View My Orders
                            </Link>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}
