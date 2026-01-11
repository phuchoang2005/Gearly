import { CreditCard, Package, ShoppingCart } from "lucide-react";

export default function CheckoutProgress({ step = "checkout" }) {
    const isCheckoutDone = step === "payment" || step === "confirmation";
    const isPaymentDone = step === "confirmation";

    return (
        <div className="flex items-center justify-center mb-10">
            <div className="flex flex-col items-center">
                <div
                    className={`
                        w-10 h-10 ${
                            step === "checkout" || isCheckoutDone ? "bg-[#1C387F]" : "bg-gray-200"
                        } rounded-full flex items-center justify-center
                    `}
                >
                    <ShoppingCart
                        className={`
                        h-5 w-5 ${
                            step === "checkout" || isCheckoutDone ? "text-white" : "text-gray-500"
                        }
                        `}
                    />
                </div>
                <span
                    className={`
                    text-sm mt-2 ${
                        step === "checkout" || isCheckoutDone ? "text-[#1C387F]" : "text-gray-500"
                    } font-semibold text-center whitespace-normal break-words w-18
                    `}
                >
                  Checkout
                </span>
            </div>

            <div className={`w-80 h-1 mx-2 ${isCheckoutDone ? "bg-[#1C387F]" : "bg-gray-300"}`}></div>

            <div className="flex flex-col items-center">
                <div
                    className={`
                    w-10 h-10 ${
                        step === "payment" || isPaymentDone ? "bg-[#1C387F]" : "bg-gray-200"
                    } rounded-full flex items-center justify-center
                    `}
                >
                    <CreditCard
                        className={`
                        h-5 w-5 ${
                            step === "payment" || isPaymentDone ? "text-white" : "text-gray-500"
                        }
                        `}
                    />
                </div>
                <span
                    className={`
                    text-sm mt-2 ${
                        step === "payment" || isPaymentDone ? "text-[#1C387F]" : "text-gray-500"
                    } font-semibold text-center whitespace-normal break-words w-17
                    `}
                >
                  Payment
                </span>
            </div>

            <div className={`w-80 h-1 mx-2 ${isPaymentDone ? "bg-[#1C387F]" : "bg-gray-300"}`}></div>

            <div className="flex flex-col items-center">
                <div
                    className={`
                    w-10 h-10 ${
                        step === "confirmation" ? "bg-[#1C387F]" : "bg-gray-200"
                    } rounded-full flex items-center justify-center
                    `}
                >
                    <Package
                        className={`
                        h-5 w-5 ${
                            step === "confirmation" ? "text-white" : "text-gray-500"
                        }
                        `}
                    />
                </div>
                <span
                    className={`
                    text-sm mt-2 ${
                        step === "confirmation" ? "text-[#1C387F]" : "text-gray-500"
                    } font-semibold text-center whitespace-normal break-words w-25
                    `}
                >
                  Order Confirmation
                </span>
            </div>
        </div>
    );
}
