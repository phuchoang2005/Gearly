import HeaderBreadcrumb from "@u_components/shared/HeaderBreadcrumb.jsx";
import { useWishlistData } from "@u_hooks/useWishlist.js";
import React, { useContext, useEffect, useMemo, useRef, useState } from "react";
import { AuthContext } from "@contexts/AuthContext.jsx";
import LoadingScreen from "@u_components/shared/LoadingScreen.jsx";
import ErrorScreen from "@u_components/shared/ErrorScreen.jsx";
import ProductGrid from "@u_pages/ShopPage/sections/ProductGrid.jsx";
import PrecomputePagination from "@u_components/shared/PrecomputePagination.jsx";
import { useQueryClient } from "@tanstack/react-query";
import { useLocation } from "react-router-dom";
import SearchAndCount from "@u_pages/ShopPage/sections/SearchAndCount.jsx";
import { useDebounce } from "use-debounce";
import { ShoppingCart } from "lucide-react";
import { showError, showPromise, showSuccess } from "@utils/toast.js";
import { bulkRemoveWishlist } from "@u_services/wishlistService.js";
import { addGuestItems, addItems } from "@u_services/cartService.js";

export default function WishlistPage() {
    const queryClient = useQueryClient();
    const location = useLocation();

    const { auth, setAuth } = useContext(AuthContext);
    const productsRef = useRef(null);
    const searchRef = useRef(null);

    const [selected, setSelected] = useState(new Set());
    const [state, setState] = useState({
        searchTxt: "",
        pageIndex: 0,
        pageSize: 12,
    });

    const [debouncedParams] = useDebounce(state, 400);
    const { data, isLoading, isError } = useWishlistData(auth, debouncedParams, setState);

    const {
        content: products = [],
        totalPages = 0,
        totalElements = 0,
        number: pageNumber = 0,
    } = data || {};

    const [allProductIds, setAllProductIds] = useState(() =>
        auth?.user?.favorites ??
        JSON.parse(localStorage.getItem("wishlist") || "[]")
    );

    const MemoHeader = useMemo(
        () => (
            <HeaderBreadcrumb
                title="Your Favorites"
                crumbs={[
                    { name: "Home", path: "/" },
                    { name: "Shop", path: "/shop" },
                    { name: "Favorites", path: "/wishlist" },
                ]}
            />
        ),
        []
    );

    const searchAndCount = useMemo(
        () => (
            <SearchAndCount
                total={totalElements}
                currentPage={state.pageIndex + 1}
                itemsPerPage={state.pageSize}
                onSearchChange={(text) =>
                    setState((prev) => ({
                        ...prev,
                        pageIndex: 0,
                        searchTxt: text,
                    }))
                }
                value={state.searchTxt}
                searchRef={searchRef}
            />
        ),
        [state.searchTxt, state.pageIndex, totalElements]
    );

    const getGuestWishlist = () =>
        JSON.parse(localStorage.getItem("wishlist") || "[]");

    useEffect(() => {
        const syncWishlist = () => {
            const guestList = getGuestWishlist();
            setAllProductIds(auth?.user?.favorites ?? guestList);
        };

        window.addEventListener("guest-wishlist-updated", syncWishlist);
        return () =>
            window.removeEventListener("guest-wishlist-updated", syncWishlist);
    }, [auth?.user]);

    useEffect(() => {
        setSelected(new Set());
    }, [debouncedParams.searchTxt]);

    useEffect(() => {
        if (searchRef.current) searchRef.current.focus();
    }, [data]);

    useEffect(() => {
        handleRemoveProduct();
    }, [location.search]);

    const isAllSelected = selected.size === allProductIds.length;

    const toggleSelectAll = () => {
        setSelected(isAllSelected ? new Set() : new Set(allProductIds));
    };

    const handleSelectToggle = (id) => {
        setSelected((prev) => {
            const next = new Set(prev);
            next.has(id) ? next.delete(id) : next.add(id);
            return next;
        });
    };

    const handleRemoveProduct = (productId) => {
        queryClient.invalidateQueries(["wishlist", state]);
        if (productId) {
            setSelected((prev) => {
                if (prev.has(productId)) prev.delete(productId);
                return prev;
            });
        }
    };

    const handleBulkRemove = async () => {
        try {
            const currentList = allProductIds.filter((f) => !selected.has(f));
            if (auth) {
                await showPromise(
                    bulkRemoveWishlist(selected),
                    {
                        loading: "Removing...",
                        success: `${selected.size} product${selected.size !== 1 ? "s" : ""} removed`,
                        error: "Error while removing items. Please try again later.",
                    },
                    { throwOnError: true }
                );
                const updatedAuth = {
                    ...auth,
                    user: {
                        ...auth.user,
                        favorites: currentList,
                    },
                };
                localStorage.setItem("auth", JSON.stringify(updatedAuth));
                setAuth(updatedAuth);
            } else {
                showSuccess(
                    `${selected.size} product${selected.size !== 1 ? "s" : ""} removed`
                );
                localStorage.setItem("wishlist", JSON.stringify(currentList));
                window.dispatchEvent(new Event("guest-wishlist-updated"));
            }
            setSelected(new Set());
            handleRemoveProduct();
        } catch (e) {
            showError("Error when trying to remove products.");
        }
    };

    const handleMoveToCart = async () => {
        try {
            if (auth) {
                await addItems(Array.from(selected));
            } else {
                await addGuestItems(
                    localStorage.getItem("guestId"),
                    Array.from(selected)
                );
            }

            await handleBulkRemove();
            queryClient.invalidateQueries({
                queryKey: ["cart", auth?.user?.id ?? "guest"],
            });

            setTimeout(() => {
                showSuccess("Selected products have been moved to your cart.");
            }, 300);
        } catch (e) {
            showError(e.response?.data?.error);
        }
    };

    const clearAll = () => {
        setState((prev) => ({
            ...prev,
            pageIndex: 0,
            searchTxt: "",
        }));
    };

    return (
        <>
            {isLoading ? (
                <LoadingScreen />
            ) : isError ? (
                <ErrorScreen />
            ) : (
                <>
                    {MemoHeader}

                    {products.length || state.searchTxt.length > 0 ? (
                        <div className="max-w-screen-lg mx-auto my-15" ref={productsRef}>
                            {searchAndCount}

                            {selected.size > 0 && (
                                <div className="flex justify-between items-center mb-4 p-3 rounded-md bg-[#FFF5F5] border border-[#FFD6D6] shadow-sm">
                                    <div className="flex items-center gap-4 text-sm text-[#D70018] font-medium">
                                        <span>
                                            {selected.size} product
                                            {selected.size !== 1 ? "s" : ""} selected
                                        </span>
                                        <button
                                            onClick={toggleSelectAll}
                                            className="text-[#D70018] hover:underline transition"
                                        >
                                            {isAllSelected
                                                ? "Deselect All"
                                                : "Select All"}
                                        </button>
                                    </div>

                                    <div className="flex gap-2">
                                        <button
                                            onClick={handleBulkRemove}
                                            className="px-4 py-2 rounded-full bg-[#D70018] hover:bg-[#B80012] text-white text-sm font-semibold shadow transition"
                                        >
                                            Remove
                                        </button>

                                        <button
                                            onClick={handleMoveToCart}
                                            className="inline-flex items-center gap-1 px-4 py-2 rounded-full bg-black hover:bg-gray-900 text-white text-sm font-semibold shadow transition"
                                        >
                                            <ShoppingCart size={14} />
                                            Move to Cart
                                        </button>
                                    </div>
                                </div>
                            )}

                            <ProductGrid
                                products={products}
                                onRemoveProduct={handleRemoveProduct}
                                clearAllFilters={clearAll}
                                selectedIds={products
                                    .map((b) => b.id)
                                    .filter((id) => selected.has(id))}
                                onToggleSelect={handleSelectToggle}
                            />

                            <PrecomputePagination
                                totalPages={totalPages}
                                currentPage={pageNumber + 1}
                                setState={setState}
                                scrollRef={productsRef}
                            />
                        </div>
                    ) : (
                        <div className="flex flex-col items-center justify-center py-24 text-center text-gray-600">
                            <h3 className="text-lg font-semibold mb-2">
                                Your wishlist is empty
                            </h3>
                            <p className="text-sm mb-6">
                                You haven’t added any products yet.
                            </p>
                            <a
                                href="/shop"
                                className="inline-block bg-[#D70018] hover:bg-[#B80012] text-white text-sm font-medium px-5 py-2.5 rounded-full transition"
                            >
                                Browse Products
                            </a>
                        </div>
                    )}
                </>
            )}
        </>
    );
}
