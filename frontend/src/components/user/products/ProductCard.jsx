import React, { useContext } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import ConditionTag from './ConditionTag.jsx';
import WishlistBtn from "@u_components/products/WishlistBtn.jsx";
import RatingStar from "@u_components/products/RatingStar.jsx";
import { useCartActions } from "@u_hooks/useCartActions.js";
import { showError } from "@utils/toast.js";
import { CheckoutContext } from "@contexts/CheckoutContext.jsx";

const ProductCard = ({
    product,
    scale = 0.9,
    onRemoveProduct,
    showCheckbox = false,
    checked = false,
    onToggle = () => {}
}) => {
    const navigate = useNavigate();
    const { setSelectedItems } = useContext(CheckoutContext);

    const authorName = product.authors?.[0] || "";
    const hasRating = product.ratingCount > 0;
    const ratingValue = hasRating ? product.averageRating : 0;
    const imageUrl = product.images?.[0]?.url || "/product-placeholder.jpg";
    const isOOS = product.stock < 1;

    const { addToCart } = useCartActions();

    const handleAddToCart = async () => {
        if (isOOS) {
            showError(`"${product.title}" is out of stock!`);
            return;
        }

        await addToCart({
            productId: product.id,
            title: product.title,
            author: authorName,
            price: product.price,
            quantity: 1,
            image: imageUrl,
            condition: product.condition,
            stock: product.stock ?? 1,
        });
    };

    const handleBuyNow = async () => {
        await handleAddToCart();

        setSelectedItems([{
            productId: product.id,
            title: product.title,
            author: authorName,
            price: product.price,
            quantity: 1,
            image: imageUrl,
            condition: product.condition,
            stock: product.stock,
        }]);

        navigate("/checkout");
    };

    return (
        <div
            className="inline-block"
            style={{ transform: `scale(${scale})`, transformOrigin: 'top left' }}
        >
            <div
                className="w-[220px] min-h-[400px] bg-[#F3F3F3]
                           border border-[#CFCFCF] rounded-[5px]
                           shadow-md hover:shadow-[0_6px_20px_rgba(0,0,0,0.3)]
                           hover:-translate-y-1 transition-all duration-300
                           p-4 flex flex-col gap-4 relative"
            >
                <div className="absolute top-3 left-3 z-10">
                    <ConditionTag type={product.condition} />
                </div>

                {showCheckbox && (
                    <div
                        onClick={onToggle}
                        className={`absolute inset-0 z-20 rounded-[5px]
                            ${checked
                                ? 'ring-4 ring-[#D70018]/60'
                                : 'hover:ring-2 hover:ring-[#D70018]/40'}
                            transition cursor-pointer`}
                    />
                )}

                <WishlistBtn productId={product.id} onRemoveProduct={onRemoveProduct} />

                <Link to={`/product/${product.id}`} className="pt-11">
                    <div className="w-full aspect-square bg-white rounded-md
                                    flex items-center justify-center p-3">
                        <img
                            src={imageUrl}
                            alt={product.title}
                            className="w-full h-full object-contain"
                        />
                    </div>
                </Link>

                <div className="text-center">
                    <Link to={`/product/${product.id}`}>
                        <h3 className="text-sm font-semibold leading-tight hover:text-[#D70018] transition">
                            {product.title}
                        </h3>
                    </Link>
                    <p className="text-xs text-gray-500 font-medium mt-1">
                        {authorName}
                    </p>
                </div>

                <div className="flex items-center justify-between px-1 mt-2">
                    {hasRating && <RatingStar ratingValue={ratingValue} />}
                    <p className="text-[#D70018] font-semibold text-sm">
                        ${product.price ? product.price.toFixed(2) : 'N/A'}
                    </p>
                </div>

                <div className="flex gap-2 mt-3 relative z-30">
                    <button
                        onClick={handleAddToCart}
                        disabled={isOOS}
                        className="flex-1 text-[10px] py-[7px] rounded-[10px]
                                   font-semibold border border-[#D70018]
                                   text-[#D70018] bg-white
                                   shadow-[0_2px_4px_rgba(0,0,0,0.45)]
                                   transition-all duration-300
                                   hover:bg-[#D70018] hover:text-white
                                   hover:scale-105
                                   disabled:opacity-70 disabled:cursor-not-allowed"
                    >
                        ADD TO CART
                    </button>

                    <button
                        onClick={handleBuyNow}
                        disabled={isOOS}
                        className="flex-1 bg-[#D70018] text-white
                                   text-[10px] py-[7px] rounded-[10px]
                                   font-semibold border border-[#D70018]
                                   shadow-[0_2px_4px_rgba(0,0,0,0.45)]
                                   transition-all duration-300
                                   hover:bg-[#B80012]
                                   hover:scale-105
                                   disabled:opacity-70 disabled:cursor-not-allowed"
                    >
                        BUY NOW
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ProductCard;
