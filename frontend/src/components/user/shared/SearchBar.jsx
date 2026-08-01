import { useState, useRef, useEffect } from "react"
import { useDebounce } from "use-debounce"
import { useNavigate, useSearchParams } from "react-router-dom"
import { useShopData } from "@u_hooks/useShopData"
import { Search, Cpu, Star, ArrowRight } from "lucide-react"
import ConditionTag from "@u_components/products/ConditionTag"

export default function SearchBar() {
    const [query, setQuery] = useState("")
    const [debouncedQuery] = useDebounce(query, 400)
    const [selectedIndex, setSelectedIndex] = useState(-1)
    const [isOpen, setIsOpen] = useState(false)
    const navigate = useNavigate()
    const [setSearchParams] = useSearchParams()
    const inputRef = useRef(null)
    const dropdownRef = useRef(null)

    const { products, isLoading } = useShopData({
        condition: "",
        minPrice: 0,
        maxPrice: 200,
        genres: "",
        sortBy: "title-az",
        search: debouncedQuery?.trim() || "",
        minRating: 0,
        page: 0,
        size: 20,
    })

    const results = products?.content || []

    useEffect(() => {
        setIsOpen(query.length > 0)
        setSelectedIndex(-1)
    }, [query])

    const handleSearch = (e) => {
        e.preventDefault()
        if (selectedIndex >= 0 && results[selectedIndex]) {
            goToProduct(results[selectedIndex].id)
        } else {
            const qs = new URLSearchParams({ search: query }).toString()
            navigate(`/shop?${qs}`)
        }
        setIsOpen(false)
        inputRef.current?.blur()
    }

    const handleKeyDown = (e) => {
        if (!isOpen) return

        switch (e.key) {
            case "ArrowDown":
                e.preventDefault()
                setSelectedIndex((prev) => (prev < results.length - 1 ? prev + 1 : prev))
                break
            case "ArrowUp":
                e.preventDefault()
                setSelectedIndex((prev) => (prev > -1 ? prev - 1 : -1))
                break
            case "Escape":
                setIsOpen(false)
                inputRef.current?.blur()
                break
        }
    }

    const goToProduct = (id) => {
        navigate(`/product/${id}`)
        setIsOpen(false)
        setQuery("")
    }

    const handleViewAllResults = () => {
        setSearchParams({ search: query })
        navigate(`/shop?search=${encodeURIComponent(query)}`)
        setIsOpen(false)
    }

    return (
        <div className="w-full max-w-xl relative">
            <form
                onSubmit={handleSearch}
                className="flex items-center h-12 w-full rounded-2xl bg-white shadow-lg ring-1 ring-gray-200
                   hover:ring-[#D70018]/40 focus-within:ring-2 focus-within:ring-[#D70018]
                   focus-within:shadow-xl transition-all duration-300 ease-out"
            >
                <div className="flex items-center pl-5 pr-2">
                    <Search size={20} className="text-gray-400" />
                </div>
                <input
                    ref={inputRef}
                    type="text"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    onFocus={() => setIsOpen(true)}
                    onBlur={() => {
                        setTimeout(() => {
                            setIsOpen(false)
                        }, 100)
                    }}
                    onKeyDown={handleKeyDown}
                    placeholder="Search for PC parts, brands..."
                    className="flex-grow px-3 py-3 text-sm text-gray-800 placeholder-gray-400 bg-transparent focus:outline-none"
                />
                <button
                    type="submit"
                    className="w-12 h-10 bg-[#D70018] hover:bg-[#B80012]
                               rounded-xl grid place-items-center transition-all duration-200 mr-1
                               shadow-md hover:shadow-lg transform hover:scale-105"
                >
                    <Search size={18} className="text-white" />
                </button>
            </form>

            {isOpen && (
                <div
                    ref={dropdownRef}
                    className="absolute top-full mt-2 w-full bg-white border border-gray-100 rounded-2xl shadow-2xl z-50
                               max-h-96 overflow-hidden backdrop-blur-sm"
                >
                    {isLoading && (
                        <div className="p-6 text-center">
                            <div className="inline-flex items-center space-x-2">
                                <div className="w-4 h-4 border-2 border-[#D70018] border-t-transparent rounded-full animate-spin"></div>
                                <span className="text-sm text-gray-600">Searching...</span>
                            </div>
                        </div>
                    )}

                    {!isLoading && results.length === 0 && (
                        <div
                            className="p-6 text-center hover:bg-gray-50 cursor-pointer transition-colors duration-200 group"
                            onClick={handleViewAllResults}
                        >
                            <div className="flex flex-col items-center space-y-3">
                                <div className="w-12 h-12 bg-gray-100 rounded-full flex items-center justify-center group-hover:bg-[#D70018]/10 transition-colors">
                                    <Search size={20} className="text-gray-400 group-hover:text-[#D70018]" />
                                </div>
                                <div>
                                    <p className="text-sm font-medium text-gray-900">No products found</p>
                                    <p className="text-xs text-gray-500 mt-1">
                                        See all results for "<span className="font-medium">{query}</span>"
                                    </p>
                                </div>
                                <div className="flex items-center text-xs text-[#D70018] font-medium group-hover:text-[#B80012]">
                                    View all results <ArrowRight size={14} className="ml-1" />
                                </div>
                            </div>
                        </div>
                    )}

                    {!isLoading && results.length > 0 && (
                        <div className="max-h-80 overflow-y-auto">
                            {results.map((product, index) => (
                                <div
                                    key={product.id}
                                    className={`flex items-center p-4 hover:bg-gray-50 cursor-pointer transition-all duration-200 border-b border-gray-50 last:border-b-0 group
                                               ${selectedIndex === index ? "bg-[#D70018]/5 border-[#D70018]/20" : ""}`}
                                    onClick={() => goToProduct(product.id)}
                                >
                                    <div className="relative">
                                        <img
                                            src={product.images[0]?.url || "/placeholder.svg"}
                                            alt={product.images[0]?.alt}
                                            className="w-12 h-16 object-cover rounded-lg shadow-sm group-hover:shadow-md transition-shadow"
                                        />
                                        <div className="absolute -top-1 -right-1 w-5 h-5 bg-[#D70018] rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                                            <Cpu size={10} className="text-white" />
                                        </div>
                                    </div>

                                    <div className="flex-1 ml-4 min-w-0">
                                        <div className="flex items-start justify-between">
                                            <div className="flex-1 min-w-0">
                                                <h4 className="text-sm font-semibold text-gray-900 truncate group-hover:text-[#B80012] transition-colors">
                                                    {product.title}
                                                </h4>
                                                <p className="text-xs text-gray-600 mt-1">{product.brand}</p>
                                                <div className="flex items-center mt-2 space-x-3">
                                                    <ConditionTag type={product.condition} />
                                                    {product.rating && (
                                                        <div className="flex items-center space-x-1">
                                                            <Star size={12} className="text-yellow-400 fill-current" />
                                                            <span className="text-xs text-gray-600">{product.rating}</span>
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                            <ArrowRight
                                                size={16}
                                                className="text-gray-400 group-hover:text-[#D70018] transition-colors ml-2 flex-shrink-0"
                                            />
                                        </div>
                                    </div>
                                </div>
                            ))}

                            {results.length > 0 && (
                                <div
                                    className="p-4 text-center border-t border-gray-100 hover:bg-gray-50 cursor-pointer transition-colors group"
                                    onClick={handleViewAllResults}
                                >
                                    <div className="flex items-center justify-center space-x-2 text-sm font-medium text-[#D70018] group-hover:text-[#B80012]">
                                        <span>View all {products?.totalElements || "results"} products</span>
                                        <ArrowRight size={16} className="group-hover:translate-x-1 transition-transform" />
                                    </div>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            )}
        </div>
    )
}
