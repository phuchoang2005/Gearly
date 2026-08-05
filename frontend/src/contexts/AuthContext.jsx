import { createContext, useState, useEffect } from "react";
import { jwtDecode } from "jwt-decode";
import {mergeWishlist} from "@u_services/wishlistService.js";
import {deleteGuestCart, getGuestCart, mergeCart} from "@u_services/cartService.js";
import {logoutUser} from "@u_services/authService.js";

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [auth, setAuth] = useState(() => {
        const stored = localStorage.getItem("auth");
        return stored ? JSON.parse(stored) : null;
    });

    const logout = async () => {
        await logoutUser();
        localStorage.removeItem("auth");
        localStorage.removeItem("cart_selected");
        setAuth(null);
    };

    const login = async (authData) => {
        localStorage.setItem("auth", JSON.stringify(authData));

        // Wishlist
        const localFav = JSON.parse(localStorage.getItem("wishlist") || "[]");
        if (localFav.length) {
            const mergedFav = await mergeWishlist(localFav);
            const favorites = mergedFav?.data.wishlist;

            authData.user = {
                ...authData.user,
                favorites
            };

            localStorage.removeItem("wishlist");
        }

        // Cart
        //
        // Folding the guest basket in is best-effort, and has to be: guest ids are signed as of
        // S12, so a visitor whose browser still holds a bare UUID from before that gets a 403
        // here. Letting it propagate would abort login before setAuth below — the sign-in would
        // look broken because of a basket the visitor may not even have anything in. The stale
        // id is dropped either way; at worst an unmergeable basket is lost.
        const guestId = localStorage.getItem("guestId");
        if (guestId) {
            try {
                const res = await getGuestCart(guestId);
                const guestItems = res.data.items || [];

                if (guestItems.length) {
                    await mergeCart(guestId, guestItems);
                } else {
                    await deleteGuestCart(guestId);
                }
            } catch (err) {
                if (err?.response?.status !== 403) throw err;
            }

            localStorage.removeItem("guestId");
        }

        // Auth
        setAuth(authData);
        setupAutoLogout(authData.token);
    };

    const setupAutoLogout = (token) => {
        try {
            const decoded = jwtDecode(token);
            const currentTime = Date.now() / 1000;
            const expiresInSeconds = decoded.exp - currentTime;

            if (expiresInSeconds <= 0) {
                logout();
                return;
            }

            setTimeout(logout, expiresInSeconds * 1000);
        } catch {
            logout();
        }
    };

    useEffect(() => {
        if (auth) {
            setupAutoLogout(auth.token);
        }
    }, []);

    return (
        <AuthContext.Provider value={{ auth, setAuth, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
};
