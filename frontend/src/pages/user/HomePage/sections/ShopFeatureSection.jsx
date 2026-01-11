import { Truck, CreditCard, Headphones } from 'lucide-react';

export default function ShopFeatureSection() {
    return (
        <div className="bg-white py-8">
            <div className="max-w-7xl mx-auto px-6 grid grid-cols-1 sm:grid-cols-3 gap-8 text-center">

                {/* ITEM 1 */}
                <div className="flex flex-col items-center gap-2 group">
                    <Truck className="w-6 h-6 text-black group-hover:text-gray-800 transition-colors" />
                    <p className="text-black font-bold group-hover:text-gray-800 transition-colors">
                        Free shipping
                    </p>
                    <p className="text-sm text-gray-600">
                        Free shipping for orders above $30
                    </p>
                </div>

                {/* ITEM 2 */}
                <div className="flex flex-col items-center gap-2 group">
                    <CreditCard className="w-6 h-6 text-black group-hover:text-gray-800 transition-colors" />
                    <p className="text-black font-bold group-hover:text-gray-800 transition-colors">
                        Flexible payment methods
                    </p>
                    <p className="text-sm text-gray-600">
                        Multiple secure payment options
                    </p>
                </div>

                {/* ITEM 3 */}
                <div className="flex flex-col items-center gap-2 group">
                    <Headphones className="w-6 h-6 text-black group-hover:text-gray-800 transition-colors" />
                    <p className="text-black font-bold group-hover:text-gray-800 transition-colors">
                        24x7 Support
                    </p>
                    <p className="text-sm text-gray-600">
                        We support online all days
                    </p>
                </div>

            </div>
        </div>
    );
}
