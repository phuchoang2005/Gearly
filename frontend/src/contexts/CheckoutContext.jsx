import { createContext, useState, useMemo, useCallback, useEffect } from 'react'
import { calcCartTotals } from '@utils/calCartTotals.js'

export const CheckoutContext = createContext({
    selectedItems: [],
    totals: {
        itemsCount: 0,
        subtotal: 0,
        shipping: 0,
        taxes: 0,
        discount: 0,
        total: 0,
    },
    shippingAddress: null,
    usingSaved: false,
    orderCompleted: false,
    setSelectedItems: () => {},
    setShippingAddress: () => {},
    setUsingSaved: () => {},
    setOrderCompleted: () => {},
})

export function CheckoutProvider({ children }) {
    const [selectedItems, setSelectedItemsState] = useState(() => {
        try {
            return JSON.parse(localStorage.getItem('cart_selected')) || []
        } catch {
            return []
        }
    })

    const [shippingAddress, setShippingAddressState] = useState(null)
    const [usingSaved, setUsingSavedState] = useState(false)
    const [orderCompleted, setOrderCompletedState] = useState(false)

    const totals = useMemo(() => calcCartTotals(selectedItems), [selectedItems])

    const setSelectedItems = useCallback(newSelected => {
        setSelectedItemsState(newSelected)
    }, [])

    const setShippingAddress = useCallback(addr => {
        setShippingAddressState(addr)
    }, [])

    const setUsingSaved = useCallback(flag => {
        setUsingSavedState(flag)
    }, [])

    const setOrderCompleted = useCallback(flag => {
        setOrderCompletedState(flag)
    }, [])

    useEffect(() => {
        const savedAddr  = JSON.parse(sessionStorage.getItem('checkout_shippingAddress') || 'null');
        if (savedAddr && typeof savedAddr === 'object') {
            setShippingAddressState(savedAddr);
            sessionStorage.removeItem('checkout_shippingAddress');
        }
    }, []);

    useEffect(() => {
        const normalized = Array.isArray(selectedItems)
            ? selectedItems
            : Object.values(selectedItems || {});
        localStorage.setItem('cart_selected', JSON.stringify(normalized));
    }, [selectedItems]);


    return (
        <CheckoutContext.Provider
            value={{
                selectedItems,
                totals,
                shippingAddress,
                usingSaved,
                orderCompleted,
                setSelectedItems,
                setShippingAddress,
                setUsingSaved,
                setOrderCompleted,
            }}
        >
            {children}
        </CheckoutContext.Provider>
    )
}
