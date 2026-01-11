import React from 'react'
import {SlidersHorizontal} from 'lucide-react'

export default function MobileFiltersToggle({openCount, mobileOpen, toggleMobile}) {
    return (
        <div className="md:hidden mb-4">
            <button
                onClick={toggleMobile}
                className="flex items-center justify-center w-full py-2 px-4 border border-gray-300 rounded-md bg-white text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
                <SlidersHorizontal className="h-4 w-4 mr-2"/>
                {mobileOpen ? 'Hide Filters' : 'Show Filters'} ({openCount} active)
            </button>
        </div>
    )
}
