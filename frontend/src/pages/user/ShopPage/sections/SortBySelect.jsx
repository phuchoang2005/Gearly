import React from 'react'

export default function SortBySelect({sortBy, setSortBy}) {
    return (
        <div>
            <h3 className="font-semibold mb-2 text-sm">Sort by</h3>
            <div className="relative">
                <select value={sortBy} onChange={e => setSortBy(e.target.value)}
                        className="w-full p-1.5 border rounded-md appearance-none bg-white pr-7 text-xs focus:ring-1 focus:ring-blue-500 focus:border-blue-500"
                >
                    <option value="title-az">Title: A to Z</option>
                    <option value="title-za">Title: Z to A</option>
                    <option value="newest">Newest</option>
                    <option value="price-low">Price: Low to High</option>
                    <option value="price-high">Price: High to Low</option>
                </select>
                <div className="absolute inset-y-0 right-0 flex items-center pr-2 pointer-events-none">
                    <svg className="w-3 h-3 text-gray-400" fill="none" stroke="currentColor"
                         viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                              d="M19 9l-7 7-7-7"/>
                    </svg>
                </div>
            </div>
        </div>
    )
}
