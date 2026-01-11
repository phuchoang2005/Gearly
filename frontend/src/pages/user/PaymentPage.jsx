import {useState, useEffect, useContext, useMemo, useCallback} from 'react'
import {useNavigate} from 'react-router-dom'
import {useQueryClient} from '@tanstack/react-query'
import {HandCoins, CheckCircle2} from 'lucide-react'

import CheckoutProgress from '@u_components/checkout/CheckoutProgress.jsx'
import OrderSummary from '@u_components/checkout/OrderSummary.jsx'
import {showError, showPromise} from '@utils/toast.js'
import {payWithCOD, payWithMomo} from '@u_services/orderService.js'
import {AuthContext} from '@contexts/AuthContext.jsx'
import {CheckoutContext} from '@contexts/CheckoutContext.jsx'

import {MomoBrandLogo, VNPayBrandLogo} from '@u_components/checkout/PaymentLogos.jsx'

const paymentMethods = [
    {
        id: 'cod',
        name: 'Cash on Delivery (COD)',
        description: 'Pay with cash when your order is delivered.',
        icon: HandCoins,
        disabled: false,
    },
    {
        id: 'momo',
        name: 'Momo E-Wallet',
        description: 'Pay securely using your Momo account.',
        icon: MomoBrandLogo,
        disabled: false,
    },
    {
        id: 'vnpay',
        name: 'VNPAY',
        description: 'Pay with Card, Bank Transfer, or VNPay QR.',
        icon: VNPayBrandLogo,
        disabled: true,
        tooltip: 'VNPAY is not supported at the moment.',
    },
]

const transformAddress = ({
    firstName,
    lastName,
    email,
    phoneNumber,
    street,
    city,
    cityId,
    state,
    stateId,
    postalCode,
    country,
    countryId
}) => ({
    firstName,
    lastName,
    email,
    phoneNumber,
    address: {street, city, cityId, state, stateId, postalCode, country, countryId},
})

export default function PaymentPage() {
    const {auth} = useContext(AuthContext)
    const {
        selectedItems,
        totals: {itemsCount, subtotal, shipping, taxes, discount, total},
        shippingAddress,
        orderCompleted,
    } = useContext(CheckoutContext)

    const navigate = useNavigate()
    const queryClient = useQueryClient()

    const cartKey = useMemo(() => ['cart', auth?.user?.id || 'guest'], [auth])
    const invalidateCart = useCallback(() => {
        queryClient.invalidateQueries({queryKey: cartKey})
    }, [queryClient, cartKey])

    const [selectedMethod, setSelectedMethod] = useState('cod')
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        if (orderCompleted || selectedItems.length === 0) {
            navigate('/cart', {replace: true})
        }
    }, [orderCompleted, selectedItems.length, navigate])

    const handlePayment = useCallback(async () => {
        if (!selectedMethod) {
            showError('Please select a payment method.')
            return
        }

        setLoading(true)

        const payload = {
            items: selectedItems.map(({bookId, quantity}) => ({bookId, quantity})),
            paymentInfo: {method: selectedMethod},
            shippingInformation: transformAddress(shippingAddress || {}),
        }

        try {
            if (selectedMethod === 'cod') {
                const {data} = await showPromise(
                    payWithCOD(payload),
                    {
                        loading: 'Processing...',
                        success: 'Order created successfully.',
                        error: 'Failed to place order. Please try again.',
                    },
                    {throwOnError: true}
                )

                navigate('/order-confirmation', {
                    replace: true,
                    state: {orderDetails: data, paymentMethod: selectedMethod},
                })
            } else if (selectedMethod === 'momo') {
                const {data} = await showPromise(
                    payWithMomo(payload),
                    {
                        loading: 'Processing...',
                        success: 'Redirecting to MoMo...',
                        error: 'Failed to initiate MoMo payment.',
                    },
                    {throwOnError: true}
                )

                sessionStorage.setItem(
                    'checkout_shippingAddress',
                    JSON.stringify(shippingAddress)
                )
                window.location.href = data.payUrl
            } else {
                showError('Invalid payment method!')
                navigate('/cart', {replace: true})
            }
        } catch (err) {
            console.error(err)
        } finally {
            invalidateCart()
            setLoading(false)
        }
    }, [selectedMethod, selectedItems, shippingAddress, invalidateCart, navigate])

    return (
        <div className="bg-neutral-100 min-h-screen">
            <div className="max-w-screen-xl mx-auto px-4 py-12">
                <CheckoutProgress step="payment"/>

                <h1 className="text-3xl font-bold text-black mt-8 mb-6">
                    Payment Options
                </h1>

                <div className="grid lg:grid-cols-7 gap-8">
                    <div className="lg:col-span-5 bg-white p-8 rounded-xl shadow-lg space-y-6">
                        <h2 className="text-xl font-semibold border-b pb-3">
                            Choose your payment method
                        </h2>

                        <div className="space-y-4">
                            {paymentMethods.map(({id, name, description, icon: Icon, disabled, tooltip}) => {
                                const isSelected = selectedMethod === id

                                return (
                                    <div
                                        key={id}
                                        role="radio"
                                        aria-checked={isSelected}
                                        title={disabled ? tooltip : undefined}
                                        onClick={() => !loading && !disabled && setSelectedMethod(id)}
                                        className={`
                                            p-5 rounded-lg flex justify-between items-center transition
                                            ${isSelected
                                                ? 'border-2 border-[#D70018] bg-[#FFF1F1]'
                                                : 'border border-gray-300 bg-white hover:border-gray-400'}
                                            ${disabled || loading ? 'opacity-60 cursor-not-allowed' : 'cursor-pointer'}
                                        `}
                                    >
                                        <div className="flex items-center">
                                            <Icon className="h-10 mr-4"/>
                                            <div>
                                                <h3 className={`font-semibold ${isSelected ? 'text-[#D70018]' : 'text-black'}`}>
                                                    {name}
                                                </h3>
                                                <p className="text-sm text-gray-500">{description}</p>
                                            </div>
                                        </div>

                                        {isSelected ? (
                                            <CheckCircle2 className="w-6 h-6 text-[#D70018]"/>
                                        ) : (
                                            <div className="w-5 h-5 border-2 border-gray-300 rounded-full"/>
                                        )}
                                    </div>
                                )
                            })}
                        </div>

                        <div className="pt-6 border-t text-right">
                            <button
                                onClick={handlePayment}
                                disabled={loading}
                                className="
                                    w-full sm:w-auto px-8 py-3 rounded-lg font-semibold
                                    bg-[#D70018] text-white
                                    hover:bg-[#B80012]
                                    transition disabled:opacity-60
                                "
                            >
                                {loading
                                    ? 'Processing...'
                                    : selectedMethod === 'cod'
                                        ? 'Place Order'
                                        : selectedMethod === 'momo'
                                            ? 'Pay with MoMo'
                                            : 'Proceed'}
                            </button>
                        </div>
                    </div>

                    <aside className="lg:col-span-2 sticky top-8">
                        <OrderSummary
                            itemsCount={itemsCount}
                            subtotal={subtotal}
                            shipping={shipping}
                            taxes={taxes}
                            discount={discount}
                            total={total}
                        />
                    </aside>
                </div>
            </div>
        </div>
    )
}
