import { useContext, useMemo, useCallback, useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { AuthContext } from '@contexts/AuthContext'
import {
    updateGuestQuantity,
    removeGuestItem,
    clearGuestCart,
    updateQuantity,
    removeItem,
    clearCart,
} from '@u_services/cartService'
import { useCartData } from '@u_hooks/useCartData'
import HeaderBreadcrumb from '@u_components/shared/HeaderBreadcrumb.jsx'
import ConditionTag from '@u_components/products/ConditionTag.jsx'
import { Minus, Plus, Trash2, ShoppingCart } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { showPromise } from '@utils/toast'
import LoadingScreen from '@u_components/shared/LoadingScreen.jsx'
import OrderSummary from '@u_components/checkout/OrderSummary.jsx'
import { CheckoutContext } from '@contexts/CheckoutContext.jsx'

export default function CartPage() {
    const { auth } = useContext(AuthContext)
    const { selectedItems, totals, setSelectedItems } = useContext(CheckoutContext)
    const qc = useQueryClient()
    const { data: cartItems = [], isLoading } = useCartData()
    const navigate = useNavigate()

    const cartKey = useMemo(() => ['cart', auth?.user?.id ?? 'guest'], [auth])

    const invalidate = useCallback(
        () => qc.invalidateQueries({ queryKey: cartKey }),
        [qc, cartKey]
    )

    const toggleSelection = useCallback(
        id => {
            const item = cartItems.find(i => i.bookId === id)
            if (!item) return
            setSelectedItems(prev => {
                const exists = prev.some(x => x.bookId === id)
                return exists ? prev.filter(x => x.bookId !== id) : [...prev, item]
            })
        },
        [cartItems, setSelectedItems]
    )

    const selectAll = useCallback(() => {
        if (cartItems.length === 0) return
        const allSelected = cartItems.every(i =>
            selectedItems.some(s => s.bookId === i.bookId)
        )
        setSelectedItems(allSelected ? [] : [...cartItems])
    }, [cartItems, selectedItems, setSelectedItems])

    const handleUpdateQuantity = useCallback(
        (id, newQty) => {
            if (newQty < 1) return
            showPromise(
                auth
                    ? () => updateQuantity(id, newQty)
                    : () => updateGuestQuantity(localStorage.getItem('guestId'), id, newQty),
                { loading: 'Updating...', success: 'Updated', error: 'Failed' }
            ).then(invalidate)
        },
        [auth, invalidate]
    )

    const handleRemoveItem = useCallback(
        id => {
            showPromise(
                auth
                    ? () => removeItem(id)
                    : () => removeGuestItem(localStorage.getItem('guestId'), id),
                { loading: 'Removing...', success: 'Removed', error: 'Failed' }
            ).then(() => {
                invalidate()
                setSelectedItems(prev => prev.filter(i => i.bookId !== id))
            })
        },
        [auth, invalidate, setSelectedItems]
    )

    const handleClearAll = useCallback(() => {
        showPromise(
            auth
                ? () => clearCart()
                : () => clearGuestCart(localStorage.getItem('guestId')),
            { loading: 'Clearing...', success: 'Cleared', error: 'Failed' }
        ).then(() => {
            invalidate()
            setSelectedItems([])
        })
    }, [auth, invalidate, setSelectedItems])

    const goToCheckout = useCallback(() => {
        if (selectedItems.length === 0) return
        navigate('/checkout')
    }, [navigate, selectedItems])

    if (isLoading) return <LoadingScreen />

    if (cartItems.length === 0) {
        return (
            <>
                <HeaderBreadcrumb
                    title="Your Cart"
                    crumbs={[
                        { name: 'Home', path: '/' },
                        { name: 'Shop', path: '/shop' },
                        { name: 'Cart', path: '/cart' },
                    ]}
                />
                <div className="max-w-screen-lg mx-auto py-24 text-center">
                    <ShoppingCart className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                    <h2 className="text-2xl font-semibold mb-2">Your cart is empty</h2>
                    <Link to="/shop" className="bg-[#D70018] text-white px-6 py-3 rounded-lg">
                        Browse Products
                    </Link>
                </div>
            </>
        )
    }

    return (
        <>
            <HeaderBreadcrumb
                title="Your Cart"
                crumbs={[
                    { name: 'Home', path: '/' },
                    { name: 'Shop', path: '/shop' },
                    { name: 'Cart', path: '/cart' },
                ]}
            />

            <div className="max-w-screen-xl mx-auto py-12 grid lg:grid-cols-7 gap-8">
                <div className="lg:col-span-5">
                    <div className="border rounded-lg overflow-hidden">
                        <div className="bg-black text-white px-6 py-4 grid grid-cols-12 gap-4 font-semibold">
                            <div className="col-span-1">
                                <input
                                    type="checkbox"
                                    onChange={selectAll}
                                    checked={cartItems.length > 0 && cartItems.every(i =>
                                        selectedItems.some(s => s.bookId === i.bookId)
                                    )}
                                />
                            </div>
                            <div className="col-span-4">PC Component</div>
                            <div className="col-span-2 text-center">Unit Price</div>
                            <div className="col-span-2 text-center">Quantity</div>
                            <div className="col-span-3 text-center">Subtotal</div>
                        </div>

                        {cartItems.map(item => (
                            <div key={item.bookId} className="grid grid-cols-12 gap-4 px-6 py-4 border-t">
                                <div className="col-span-1 flex items-center">
                                    <input
                                        type="checkbox"
                                        checked={selectedItems.some(s => s.bookId === item.bookId)}
                                        onChange={() => toggleSelection(item.bookId)}
                                    />
                                </div>

                                <div className="col-span-4 flex gap-4">
                                    <img src={item.image} className="w-20 h-20 object-contain" />
                                    <div>
                                        <h3 className="font-semibold">{item.title}</h3>
                                        <ConditionTag type={item.condition} />
                                    </div>
                                </div>

                                <div className="col-span-2 text-center font-semibold">
                                    ${item.price.toFixed(2)}
                                </div>

                                <div className="col-span-2 flex justify-center gap-2">
                                    <button onClick={() => handleUpdateQuantity(item.bookId, item.quantity - 1)}>
                                        <Minus />
                                    </button>
                                    <span>{item.quantity}</span>
                                    <button onClick={() => handleUpdateQuantity(item.bookId, item.quantity + 1)}>
                                        <Plus />
                                    </button>
                                </div>

                                <div className="col-span-3 text-center font-semibold">
                                    ${(item.price * item.quantity).toFixed(2)}
                                    <button
                                        onClick={() => handleRemoveItem(item.bookId)}
                                        className="block text-sm text-[#D70018] mt-1"
                                    >
                                        Remove
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>

                    <button onClick={handleClearAll} className="mt-4 text-[#D70018]">
                        Clear All
                    </button>
                </div>

                <aside className="lg:col-span-2">
                    <OrderSummary {...totals} goToCheckout={goToCheckout} />
                </aside>
            </div>
        </>
    )
}
