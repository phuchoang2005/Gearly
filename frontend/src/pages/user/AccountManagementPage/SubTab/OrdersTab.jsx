import { useState, useRef } from "react";
import { ShoppingBag, Search, AlertTriangle } from "lucide-react";
import { useOutletContext } from "react-router-dom";
import { OrderFilters } from "@u_components/orders/OrderFilters.jsx";
import { OrderCard } from "@u_components/orders/OrderCard.jsx";
import { ReviewModal } from "@u_components/modal/ReviewModal.jsx";
import { CancelOrderModal } from "@u_components/modal/CancelOrderModal.jsx";
import PrecomputePagination from "@u_components/shared/PrecomputePagination.jsx";
import { useDebounce } from "use-debounce";
import { useOrders } from "@u_hooks/useOrders.js";
import {showPromise} from "@utils/toast.js";
import {cancelOrder} from "@u_services/orderService.js";
import {submitReview} from "@u_services/reviewService.js";
import {useQueryClient} from "@tanstack/react-query";

const ORDER_STATUSES = [
    { value: "", label: "All Orders", count: 0 },
    { value: "PENDING", label: "Pending", count: 0 },
    { value: "PROCESSING", label: "Processing", count: 0 },
    { value: "SHIPPED", label: "Shipped", count: 0 },
    { value: "DELIVERED", label: "Delivered", count: 0 },
    { value: "COMPLETED", label: "Completed", count: 0 },
    { value: "CANCELLED", label: "Cancelled", count: 0 },
    { value: "PENDING_REFUND", label: "Pending Refund", count: 0 },
    { value: "REFUNDED", label: "Refunded", count: 0 },
];

export default function OrdersTab() {
    const scrollRef = useRef(null);
    const { statsData } = useOutletContext();
    const qc = useQueryClient();

    const [searchTerm, setSearchTerm] = useState("");
    const [selectedStatus, setSelectedStatus] = useState("");
    const [pageIndex, setPageIndex] = useState(0);

    const [debouncedSearchTerm] = useDebounce(searchTerm, 500);

    const {
        data: ordersData,
        isLoading: ordersLoading,
        isError: ordersError,
    } = useOrders({
        pageIndex,
        status: selectedStatus,
        search: debouncedSearchTerm,
    });

    const orders = ordersData?.content || [];
    const totalPages = ordersData?.totalPages || 0;
    const totalElements = ordersData?.totalElements || 0;

    const [reviewModal, setReviewModal] = useState({ isOpen: false, order: null });
    const [cancelModal, setCancelModal] = useState({ isOpen: false, order: null });

    const handleSearch = (value) => {
        setSearchTerm(value);
        setPageIndex(0);
    };

    const handleStatusFilter = (status) => {
        setSelectedStatus(status);
        setPageIndex(0);
    };

    const handleWriteReview = (order) => {
        setReviewModal({ isOpen: true, order });
    };

    const handleCancelOrder = (order) => {
        setCancelModal({ isOpen: true, order });
    };

    const handleReviewSubmit = async (reviewData) => {
        try {
            await showPromise(
                submitReview(reviewData),
                {
                    loading: 'Submitting...',
                    success: 'Awesome! Your review is in, and we\'ll check it out before it goes live.',
                    error: 'Failed to submit review for this order, please try again later.',
                }
            );
            qc.invalidateQueries({queryKey: ["orders", pageIndex, selectedStatus, debouncedSearchTerm]})
            setReviewModal({ isOpen: false, order: null });
        } catch(e) {
            console.error(e);
        }
    };

    const handleOrderCancel = async (orderId, reason) => {
        try {
            await showPromise(
                cancelOrder({
                    orderId,
                    reason
                }),
                {
                    loading: 'Cancelling...',
                    success: 'Order has been cancelled.',
                    error: 'Failed to cancel this order, please try again later.',
                }
            );
            qc.invalidateQueries({queryKey: ["orders", pageIndex, selectedStatus, debouncedSearchTerm]})
            qc.invalidateQueries({queryKey: ["orderStats"]})
            setCancelModal({ isOpen: false, order: null });
        } catch(e) {
            console.error(e);
        }
    };

    const handleSetState = (updater) => {
        const newState = updater({ pageIndex });
        setPageIndex(newState.pageIndex);
    };

    // Merge stats into statuses
    const statusesWithCounts = ORDER_STATUSES.map((entry) => ({
        ...entry,
        count: statsData?.[entry.value] || 0,
    }));

    return (
        <div ref={scrollRef} className="relative space-y-6">
            {/* Overlay loading indicator */}
            {ordersLoading && (
                <div className="absolute inset-0 bg-white/80 backdrop-blur-sm z-10 flex items-center justify-center">
                    <div className="animate-spin rounded-full h-8 w-8 border-2 border-[#1C387F] border-t-transparent"></div>
                    <span className="ml-3 text-gray-600">Loading orders...</span>
                </div>
            )}

            {/* Header */}
            <div className="flex items-center justify-between">
                <h3 className="text-xl font-bold text-gray-900 flex items-center">
                    <ShoppingBag className="mr-2 text-[#1C387F]" size={20} />
                    My Orders
                </h3>
                <p className="text-sm text-gray-500">
                    {totalElements} order{totalElements !== 1 ? "s" : ""} found
                </p>
            </div>

            {/* Search Input */}
            <div className="relative">
                <Search
                    className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400"
                    size={20}
                />
                <input
                    type="text"
                    placeholder="Search orders by ID, item name, or recipient..."
                    value={searchTerm}
                    onChange={(e) => handleSearch(e.target.value)}
                    className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#1C387F] focus:border-[#1C387F] transition-colors"
                />
            </div>

            {/* Filter Buttons */}
            <OrderFilters
                statuses={statusesWithCounts}
                selectedStatus={selectedStatus}
                onStatusChange={handleStatusFilter}
            />

            {/* Error Message */}
            {ordersError && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center">
                    <AlertTriangle className="text-red-600 mr-3" size={20} />
                    <span className="text-red-700">Failed to load orders. Please try again.</span>
                </div>
            )}

            {/* Orders List */}
            {!ordersError && (
                <div className="space-y-4">
                    {orders.length === 0 && !ordersLoading && (
                        <div className="text-center py-12">
                            <ShoppingBag className="mx-auto text-gray-400 mb-4" size={48} />
                            <h3 className="text-lg font-semibold text-gray-900 mb-2">
                                {searchTerm || selectedStatus
                                    ? "No matching orders found"
                                    : "No orders yet"}
                            </h3>
                            <p className="text-gray-500">
                                {searchTerm || selectedStatus
                                    ? "Try adjusting your search or filter criteria."
                                    : "When you place your first order, it will appear here."}
                            </p>
                        </div>
                    )}

                    {orders.map((order) => (
                        <OrderCard
                            key={order.id}
                            order={order}
                            onWriteReview={handleWriteReview}
                            onCancelOrder={handleCancelOrder}
                        />
                    ))}
                </div>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
                <PrecomputePagination
                    totalPages={totalPages}
                    currentPage={pageIndex + 1}
                    setState={handleSetState}
                    scrollRef={scrollRef}
                />
            )}

            {/* Modals */}
            <ReviewModal
                key={reviewModal.order?.id}
                isOpen={reviewModal.isOpen}
                order={reviewModal.order}
                onClose={() => setReviewModal({ isOpen: false, order: null })}
                onSubmit={handleReviewSubmit}
            />

            <CancelOrderModal
                key={cancelModal.order?.id}
                isOpen={cancelModal.isOpen}
                order={cancelModal.order}
                onClose={() => setCancelModal({ isOpen: false, order: null })}
                onConfirm={handleOrderCancel}
            />
        </div>
    );
}
