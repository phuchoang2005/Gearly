import { useContext, useCallback } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { AuthContext } from "@contexts/AuthContext.jsx";
import { addItem, addGuestItem } from "@u_services/cartService";
import { showPromise } from "@utils/toast.js";

export function useCartActions() {
    const { auth } = useContext(AuthContext);
    const qc = useQueryClient();

    const addToCart = useCallback(
        (cartItem) => {
            return showPromise(
                () =>
                    auth
                        ? addItem(cartItem)
                        : addGuestItem(localStorage.getItem("guestId"), cartItem),
                {
                    loading: "Adding to cart...",
                    success: "Added to cart!",
                    error: (err) =>
                        err?.response?.data?.error || "Failed to add to cart.",
                }
            ).then(() => qc.invalidateQueries({ queryKey: ["cart", auth?.user?.id ?? 'guest'] }));
        },
        [auth, qc]
    );

    return { addToCart };
}
