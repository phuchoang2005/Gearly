import React from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'

export default function Pagination({ totalPages, currentPage, onPageChange, onPrev, onNext, pageIndex }) {
    if (totalPages <= 1) return null;

    return (
        <div className="mt-6">
            <div className="flex items-center justify-center">
                <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
                    <button
                        onClick={onPrev}
                        disabled={currentPage <= 1}
                        className={`relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed`}
                    >
                        <ChevronLeft className="h-5 w-5" aria-hidden="true"/>
                    </button>

                    {pageIndex.map((page, index) => {
                        if (page === "...") {
                            return (
                                <span
                                    key={index + '...'}
                                    className="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700 cursor-pointer"
                                >
                                    ...
                                </span>
                            )
                        }

                        return (
                            <button
                                key={page}
                                onClick={() => onPageChange(page)}
                                className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium ${
                                    currentPage === page
                                        ? 'z-10 bg-blue-50 border-blue-500 text-blue-600'
                                        : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
                                }`}
                                aria-current={currentPage === page ? 'page' : undefined}
                            >
                                {page}
                            </button>
                        )
                    })}

                    <button
                        onClick={onNext}
                        disabled={currentPage >= totalPages}
                        className={`relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed`}
                    >
                        <ChevronRight className="h-5 w-5" aria-hidden="true"/>
                    </button>
                </nav>
            </div>
            <div className="mt-2 text-xs text-center text-gray-500">
                Page {currentPage} of {totalPages}
            </div>
        </div>
    )
}
