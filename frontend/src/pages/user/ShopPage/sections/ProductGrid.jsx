import React from 'react'
import ProductCard from '@u_components/products/ProductCard'

export default function ProductGrid({
    products,
    clearAllFilters,
    emptyContent,
    onRemoveProduct,
    selectedIds = [],
    onToggleSelect
}) {
    if (!products.length) {
        return (
            emptyContent || (
                <div className="text-center py-10 bg-gray-50 rounded-lg border-2 border-gray-300 shadow-md">
                    <h3 className="text-lg font-semibold text-gray-900">
                        No PC components found
                    </h3>
                    <p className="mt-2 text-sm text-gray-600">
                        Try adjusting your filters or search criteria.
                    </p>
                    <button
                        onClick={clearAllFilters}
                        className="mt-4 px-5 py-2 bg-black text-white rounded-md
                                   hover:bg-gray-800 transition-colors"
                    >
                        Clear all filters
                    </button>
                </div>
            )
        )
    }

    return (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {products.map(product => (
                <div className="relative" key={product.id}>
                    <ProductCard
                        product={product}
                        onRemoveProduct={onRemoveProduct}
                        showCheckbox={!!onToggleSelect}
                        checked={selectedIds.includes(product.id)}
                        onToggle={() => onToggleSelect(product.id)}
                    />
                </div>
            ))}
        </div>
    )
}
