import { Heart } from "lucide-react";
import React, { useContext, useEffect, useState } from "react";
import { AuthContext } from "@contexts/AuthContext";
import { addToWishlist, removeFromWishlist } from "@u_services/wishlistService";
import { showPromise, showSuccess } from "@utils/toast.js";

export default function WishlistBtn({ productId, onRemoveProduct }) {
    const { auth, setAuth } = useContext(AuthContext);
    const [liked, setLiked] = useState(false);

    useEffect(() => {
        if (auth?.user?.favorites) {
            setLiked(auth.user.favorites.includes(productId));
        } else {
            const guestList = JSON.parse(localStorage.getItem("wishlist") || "[]");
            setLiked(guestList.includes(productId));
        }
    }, [auth, productId]);

    const toggleWishlist = async () => {
        const isGuest = !auth?.user;
        const currentList = new Set(
            isGuest
                ? JSON.parse(localStorage.getItem("wishlist") || "[]")
                : auth.user.favorites || []
        );

        const isAlreadyInList = currentList.has(productId);
        const isAdding = !isAlreadyInList;

        const updateGuestWishlist = () => {
            localStorage.setItem("wishlist", JSON.stringify([...currentList]));
            window.dispatchEvent(new Event("guest-wishlist-updated"));
        };

        if (isGuest) {
            if (isAlreadyInList) {
                currentList.delete(productId);
                showSuccess("Removed from wishlist");
                updateGuestWishlist();
                onRemoveProduct?.(productId);
            } else {
                currentList.add(productId);
                showSuccess("Added to wishlist");
                updateGuestWishlist();
            }
            setLiked(isAdding);
            return;
        }

        const action = async () => {
            if (isAlreadyInList) {
                await removeFromWishlist(productId);
                currentList.delete(productId);
                onRemoveProduct?.(productId);
            } else {
                await addToWishlist(productId);
                currentList.add(productId);
            }

            const updatedAuth = {
                ...auth,
                user: {
                    ...auth.user,
                    favorites: [...currentList],
                },
            };

            localStorage.setItem("auth", JSON.stringify(updatedAuth));
            setAuth(updatedAuth);
            setLiked(isAdding);
        };

        await showPromise(action, {
            loading: isAdding ? "Adding to wishlist..." : "Removing from wishlist...",
            success: isAdding ? "Added to wishlist" : "Removed from wishlist",
            error: "Failed to update wishlist",
        });
    };

    return (
        <button
            onClick={toggleWishlist}
            className="absolute top-3 right-3 z-50
                       w-7 h-7 bg-white rounded-full
                       shadow-[0_2px_4px_rgba(0,0,0,0.45)]
                       flex items-center justify-center
                       transition-all duration-300
                       hover:scale-110
                       hover:shadow-[0_4px_10px_rgba(0,0,0,0.60)]"
        >
            <Heart
                className={`w-4 h-4 ${
                    liked
                        ? "text-[#D70018] fill-[#D70018]"
                        : "text-black"
                }`}
            />
        </button>
    );
}
