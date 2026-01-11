import { Heart } from "lucide-react";
import React, { useContext, useEffect, useState } from "react";
import { AuthContext } from "@contexts/AuthContext";
import { addToWishlist, removeFromWishlist } from "@u_services/wishlistService";
import { showPromise, showSuccess } from "@utils/toast.js";

export default function WishlistBtn({ bookId, onRemoveBook }) {
    const { auth, setAuth } = useContext(AuthContext);
    const [liked, setLiked] = useState(false);

    useEffect(() => {
        if (auth?.user?.favorites) {
            setLiked(auth.user.favorites.includes(bookId));
        } else {
            const guestList = JSON.parse(localStorage.getItem("wishlist") || "[]");
            setLiked(guestList.includes(bookId));
        }
    }, [auth, bookId]);

    const toggleWishlist = async () => {
        const isGuest = !auth?.user;
        const currentList = new Set(
            isGuest
                ? JSON.parse(localStorage.getItem("wishlist") || "[]")
                : auth.user.favorites || []
        );

        const isAlreadyInList = currentList.has(bookId);
        const isAdding = !isAlreadyInList;

        const updateGuestWishlist = () => {
            localStorage.setItem("wishlist", JSON.stringify([...currentList]));
            window.dispatchEvent(new Event("guest-wishlist-updated"));
        };

        if (isGuest) {
            if (isAlreadyInList) {
                currentList.delete(bookId);
                showSuccess("Removed from wishlist");
                updateGuestWishlist();
                onRemoveBook?.(bookId);
            } else {
                currentList.add(bookId);
                showSuccess("Added to wishlist");
                updateGuestWishlist();
            }
            setLiked(isAdding);
            return;
        }

        const action = async () => {
            if (isAlreadyInList) {
                await removeFromWishlist(bookId);
                currentList.delete(bookId);
                onRemoveBook?.(bookId);
            } else {
                await addToWishlist(bookId);
                currentList.add(bookId);
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
