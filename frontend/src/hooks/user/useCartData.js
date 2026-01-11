import { useQuery } from '@tanstack/react-query';
import {
    initGuestCart,
    getGuestCart,
    getCart,
} from '@u_services/cartService.js';
import { useContext } from 'react';
import { AuthContext } from '@contexts/AuthContext.jsx';

export function useCartData() {
    const { auth } = useContext(AuthContext);

    return useQuery({
        queryKey: ['cart', auth?.user?.id ?? 'guest'],
        queryFn: async () => {
            if (!auth) {
                let guestId = localStorage.getItem('guestId');
                if (!guestId) {
                    guestId = await initGuestCart();
                    localStorage.setItem('guestId', guestId);
                }
                const res = await getGuestCart(guestId);
                return res.data.items || [];
            } else {
                const res = await getCart();
                return res.data.items || [];
            }
        },
        keepPreviousData: true,
    });
}
