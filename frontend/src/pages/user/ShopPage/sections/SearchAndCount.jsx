import React from 'react'
import { Search } from 'lucide-react'

export default function SearchAndCount({
    total,
    currentPage,
    itemsPerPage,
    onSearchChange,
    value,
    searchRef
}) {
    const start = total > 0 ? (currentPage - 1) * itemsPerPage + 1 : 0
    const end = Math.min(currentPage * itemsPerPage, total)

    return (
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 sticky top-0 z-10 bg-white py-2">
            <p className="text-sm mb-2 sm:mb-0 text-gray-700">
                Showing {start}-{end} of {total} products
            </p>

            <div className="flex items-center gap-2 ml-auto">
                <div className="relative w-55">
                    <input
                        ref={searchRef}
                        type="text"
                        value={value}
                        onChange={e => onSearchChange(e.target.value)}
                        placeholder="Search PC components..."
                        className="w-full p-1.5 pl-7 border border-gray-300 rounded-md text-sm
                                   focus:ring-1 focus:ring-black focus:border-black"
                    />
                    <Search className="absolute left-2 top-2 h-3.5 w-3.5 text-gray-400" />
                </div>
            </div>
        </div>
    )
}
