import {useContext, useEffect} from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { showError } from "@utils/toast.js";
import { getOrderById, notifyMomoPaymentStatus } from "@u_services/orderService.js";
import {CheckoutContext} from "@contexts/CheckoutContext.jsx";

export default function MomoReturnPage() {
    const navigate = useNavigate();
    const { search } = useLocation();

    const { setSelectedItems, setUsingSaved, setShippingAddress, setOrderCompleted } = useContext(CheckoutContext);

    useEffect(() => {
        const params = new URLSearchParams(search);
        const momoOrderId = params.get("orderId");
        if (!momoOrderId) {
            showError("This path is not valid!");
            navigate("/cart", { replace: true });
            return;
        }
        const ourOrderId = momoOrderId.replace(/^Bookify-/, "");

        const payload = {
            partnerCode: params.get("partnerCode"),
            requestId: params.get("requestId"),
            amount: params.get("amount"),
            orderId: params.get("orderId"),
            orderInfo: params.get("orderInfo"),
            orderType: params.get("orderType"),
            transId: params.get("transId"),
            resultCode: params.get("resultCode"),
            message: params.get("message"),
            responseTime: params.get("responseTime"),
            payType: params.get("payType"),
            extraData: params.get("extraData"),
            signature: params.get("signature"),
        };

        notifyMomoPaymentStatus(payload)
            .then(() => getOrderById(ourOrderId))
            .then(res => {
                const order = res.data;

                navigate("/order-confirmation", {
                    replace: true,
                    state: {
                        orderDetails: order,
                        paymentMethod: "momo",
                    },
                });
            })
            .catch(() => {
                showError("Unable to process payment at this moment. Please try again later.");
                setUsingSaved(false);
                setSelectedItems([]);
                setShippingAddress(null);
                setOrderCompleted(true);

                navigate("/cart", { replace: true });
            });
    }, [search, navigate]);

    return (
        <div className="flex items-center justify-center min-h-screen">
            <p className="text-gray-700">Processing payment…</p>
        </div>
    );
}
