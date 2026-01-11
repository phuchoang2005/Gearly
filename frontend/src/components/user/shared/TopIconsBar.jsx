import { Link, useNavigate } from "react-router-dom";
import { Heart, User, ShoppingCart } from "lucide-react";
import { AuthContext } from "@contexts/AuthContext.jsx";
import { useContext, useEffect, useState } from "react";
import { showSuccess } from "@utils/toast.js";
import { useCartData } from "@u_hooks/useCartData";
import {useQueryClient} from "@tanstack/react-query";
import {UserAvatar} from "@u_components/profile/UserAvatar.jsx";

export default function TopIconsBar({ color }) {
    const { auth, logout } = useContext(AuthContext);
    const navigate = useNavigate();

    const qc = useQueryClient();

    useEffect(() => {
        qc.invalidateQueries({ queryKey: ['cart', auth?.user?.id ?? 'guest'] });
    }, [auth]);

    const { data: cartItems = [] } = useCartData(auth);
    const cartCount = cartItems.reduce((sum, item) => sum + item.quantity, 0);

    const [guestWishlistCount, setGuestWishlistCount] = useState(() =>
        JSON.parse(localStorage.getItem("wishlist") || "[]").length
    );

    useEffect(() => {
        const updateGuestCount = () => {
            const guestList = JSON.parse(localStorage.getItem("wishlist") || "[]");
            setGuestWishlistCount(guestList.length);
        };

        window.addEventListener("guest-wishlist-updated", updateGuestCount);

        return () => {
            window.removeEventListener("guest-wishlist-updated", updateGuestCount);
        };
    }, []);

    const wishlistCount = auth?.user?.favorites?.length ?? guestWishlistCount;

    const icons = [
        {
            to: "/wishlist",
            icon: (
                <div className="relative">
                    <Heart size={22} strokeWidth={1.5} />
                    {wishlistCount > 0 && (
                        <span className={`absolute -top-1 ${wishlistCount > 9 ? "-right-4" : "-right-2"} bg-red-500 text-white text-xs rounded-full px-1.5 py-0.5 leading-none`}>
                            {wishlistCount}
                        </span>
                    )}
                </div>
            ),
        },
        {
            to: "/cart",
            icon: (
                <div className="relative">
                    <ShoppingCart size={22} strokeWidth={1.5} />
                    {cartCount > 0 && (
                        <span className={`absolute -top-1 ${cartCount > 9 ? "-right-4" : "-right-2"} bg-red-500 text-white text-xs rounded-full px-1.5 py-0.5 leading-none`}>
                            {cartCount}
                        </span>
                    )}
                </div>
            ),
        },
    ];

    const getInitial = () => {
        const fullName = auth?.user?.fullName;
        return fullName ? fullName.charAt(0).toUpperCase() : "U";
    };

    return (
        <div className="flex items-center gap-12 pl-4 relative">
            {icons.map(({ to, icon }, idx) => (
                <Link
                    key={idx}
                    to={to}
                    className={`hover:scale-110 transition-transform duration-150 ${color}`}
                >
                    {icon}
                </Link>
            ))}

            {!auth ? (
                <Link to="/login" className={`hover:scale-110 transition-transform duration-150 ${color}`}>
                    <User size={22} strokeWidth={1.5} />
                </Link>
            ) : (
                <div className="relative group">
                    <UserAvatar
                        profileAvatar={auth?.user?.profileAvatar}
                        name={getInitial()}
                        onClick={() => {navigate('/me')}}
                        className="hover:scale-105"
                    />

                    <div className="absolute right-0 mt-2 w-56 bg-white shadow-xl rounded-xl z-30 text-sm text-gray-800 overflow-hidden border opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-150">
                        <Link to="/me" className="block px-5 py-3 hover:bg-gray-100 transition">
                            My Account
                        </Link>
                        <Link to="/me/security" state={{ tab: "security" }} className="block px-5 py-3 hover:bg-gray-100 transition">
                            Change Password
                        </Link>
                        <Link to="/me/orders" state={{ tab: "order" }} className="block px-5 py-3 hover:bg-gray-100 transition">
                            Order History
                        </Link>
                        <button
                            onClick={() => {
                                logout();
                                showSuccess("You have been logged out.");
                                navigate("/");
                            }}
                            className="w-full text-left px-5 py-3 hover:bg-gray-100 transition border-t bg-red-50 text-red-600 cursor-pointer"
                        >
                            Logout
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}
