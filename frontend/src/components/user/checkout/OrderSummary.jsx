export default function OrderSummary({
    itemsCount,
    subtotal,
    shipping,
    taxes,
    discount,
    total,
    goToCheckout,
    goToPayment
}) {
    return (
        <div className="bg-white rounded-lg border-2 border-[#D70018] shadow-xl overflow-hidden sticky top-4">
            <div className="bg-[#D70018] text-white px-6 py-4">
                <h2 className="font-semibold text-lg">Order Summary</h2>
            </div>

            <div className="p-6 space-y-4 text-sm">
                <div className="flex justify-between">
                    <span>Item(s):</span>
                    <span>{itemsCount}</span>
                </div>

                <div className="flex justify-between">
                    <span>Subtotal:</span>
                    <span>${subtotal.toFixed(2)}</span>
                </div>

                <div className="flex justify-between">
                    <span>Taxes (8%):</span>
                    <span>${taxes.toFixed(2)}</span>
                </div>

                {shipping === 0 ? (
                    <div className="flex justify-between text-green-600 font-medium">
                        <span>Free shipping:</span>
                        <span>$0.00</span>
                    </div>
                ) : (
                    <div>
                        <div className="flex justify-between">
                            <span>Shipping:</span>
                            <span>${shipping.toFixed(2)}</span>
                        </div>
                        <div className="text-xs text-gray-500 text-center mt-2">
                            Free shipping on orders over $30
                        </div>
                    </div>
                )}

                <div className="flex justify-between text-green-600 font-medium">
                    <span>Sale discount:</span>
                    <span>- ${discount.toFixed(2)}</span>
                </div>

                <hr className="border-gray-200" />

                <div className="flex justify-between font-semibold text-lg">
                    <span>Total:</span>
                    <span>${total.toFixed(2)}</span>
                </div>

                {goToCheckout ? (
                    <button
                        onClick={goToCheckout}
                        disabled={itemsCount === 0}
                        className="w-full bg-[#D70018] hover:bg-[#B80012] text-white py-3 rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        Proceed to Checkout
                    </button>
                ) : goToPayment ? (
                    <button
                        type="submit"
                        className="w-full bg-[#D70018] hover:bg-[#B80012] text-white py-3 rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        Continue to Payment
                    </button>
                ) : null}

                {itemsCount === 0 && goToCheckout && (
                    <p className="mt-4 text-xs text-gray-500 text-center">
                        Select items to proceed to checkout
                    </p>
                )}
            </div>
        </div>
    )
}
