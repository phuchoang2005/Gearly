import { useQuery } from '@tanstack/react-query';
import {
    initGuestCart,
    getGuestCart,
    getCart,
} from '@u_services/cartService.js';
import { useContext } from 'react';
import { AuthContext } from '@contexts/AuthContext.jsx';

/**
 * Read the guest basket, starting a new one if the stored id is not usable.
 *
 * The backend signs guest ids as of S12 and refuses any it did not issue with a 403 — see
 * GuestCartIds. That is not an error to show: it happens once to a returning visitor whose
 * browser still holds a bare UUID from before the change, and the right response is to forget
 * that id and start over. Only the load path needs this; every mutation runs after a successful
 * load, so by then the stored id is one the server just accepted.
 *
 * Retried exactly once. A second 403 means the id we were just handed was rejected, which is a
 * real failure and should surface rather than loop.
 */
async function loadGuestCart() {
    let guestId = localStorage.getItem('guestId');
    if (!guestId) {
        guestId = await initGuestCart();
        localStorage.setItem('guestId', guestId);
    }

    try {
        return await getGuestCart(guestId);
    } catch (err) {
        if (err?.response?.status !== 403) throw err;

        localStorage.removeItem('guestId');
        const fresh = await initGuestCart();
        localStorage.setItem('guestId', fresh);
        return getGuestCart(fresh);
    }
}

export function useCartData() {
    const { auth } = useContext(AuthContext);

    return useQuery({
        queryKey: ['cart', auth?.user?.id ?? 'guest'],
        queryFn: async () => {
            if (!auth) {
                const res = await loadGuestCart();
                return res.data.items || [];
            } else {
                const res = await getCart();
                return res.data.items || [];
            }
        },
        keepPreviousData: true,
    });
}
