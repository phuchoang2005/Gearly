import React from 'react'
import {X} from 'lucide-react'

export default function ActiveFilters({activeFilters, removeFilter, clearAll}) {
    if (!activeFilters.length) return null
    return (
        <div className="flex flex-wrap gap-1.5 items-center mb-4">
            {activeFilters.map((filter, index) => (
                <div key={index} className="flex items-center bg-gray-100 rounded-full px-2 py-0.5 text-xs">
                    {filter.value}
                    <button onClick={() => removeFilter(filter.type)}
                            className="ml-1 text-gray-500 hover:text-gray-700">
                        <X className="h-3 w-3"/>
                    </button>
                </div>
            ))}
            <button onClick={clearAll} className="text-blue-600 text-xs font-medium ml-1">
                Clear All
            </button>
        </div>
    )
}
