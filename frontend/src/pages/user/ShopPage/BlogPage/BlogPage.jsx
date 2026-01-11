import {useMemo, useState} from "react"
import {useQuery} from "@tanstack/react-query"
import {fetchBlogSummaries} from "@u_services/blogService.js"
import {Link} from "react-router-dom"
import {Search, Calendar, User, Tag, ArrowRight} from "lucide-react"

export default function BlogPage() {
    const [searchTerm, setSearchTerm] = useState("")
    const [selectedTag, setSelectedTag] = useState("")

    const {
        data: blogPosts,
        isLoading,
        isError,
        error,
    } = useQuery({
        queryKey: ["blogSummaries"],
        queryFn: fetchBlogSummaries,
        staleTime: 1000 * 60 * 5,
    })

    const filteredPosts = useMemo(() => {
        if (!blogPosts) return []
        return blogPosts.filter((post) => {
            const matchesSearch =
                post.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
                post.author.toLowerCase().includes(searchTerm.toLowerCase())
            const matchesTag = !selectedTag || (post.tags && post.tags.includes(selectedTag))
            return matchesSearch && matchesTag
        })
    }, [blogPosts, searchTerm, selectedTag])

    const allTags = useMemo(() => {
        if (!blogPosts) return []
        const tags = new Set()
        blogPosts.forEach((post) => {
            if (post.tags) {
                post.tags.forEach((tag) => tags.add(tag))
            }
        })
        return Array.from(tags)
    }, [blogPosts])

    if (isLoading) {
        return (
            <>
                <div className="min-h-screen bg-gray-50">
                    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
                        <div className="text-center">
                            <div
                                className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-[#1C387F] mb-4"></div>
                            <p className="text-gray-600 text-lg">Loading blog posts...</p>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 mt-12">
                            {[...Array(6)].map((_, i) => (
                                <div key={i}
                                     className="bg-white rounded-lg shadow-sm border border-gray-100 p-6 animate-pulse">
                                    <div className="h-4 bg-gray-200 rounded w-3/4 mb-4"></div>
                                    <div className="h-3 bg-gray-200 rounded w-1/2 mb-2"></div>
                                    <div className="h-3 bg-gray-200 rounded w-1/3 mb-4"></div>
                                    <div className="flex gap-2 mb-4">
                                        <div className="h-6 bg-gray-200 rounded-full w-16"></div>
                                        <div className="h-6 bg-gray-200 rounded-full w-20"></div>
                                    </div>
                                    <div className="h-10 bg-gray-200 rounded w-full"></div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </>
        )
    }

    if (isError) {
        return (
            <>
                <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                    <div className="text-center">
                        <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-4">
                            <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={2}
                                    d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                                />
                            </svg>
                        </div>
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">Error Loading Blog Posts</h3>
                        <p className="text-red-500">{error.message}</p>
                    </div>
                </div>
            </>
        )
    }

    return (
        <>
            <div className="min-h-screen bg-gray-50">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
                    <div className="text-center mb-12">
                        <h1 className="text-4xl font-bold text-gray-900 mb-4">Our Latest Blog Posts</h1>
                        <p className="text-xl text-gray-600 max-w-2xl mx-auto">
                            Discover insights, tutorials, and stories from our community
                        </p>
                    </div>

                    <div className="mb-8 space-y-4 lg:space-y-0 lg:flex lg:items-center lg:justify-between">
                        <div className="relative flex-1 max-w-md">
                            <Search
                                className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5"/>
                            <input
                                type="text"
                                placeholder="Search posts..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className="w-full pl-10 pr-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-[#1C387F] focus:border-transparent outline-none transition-all"
                            />
                        </div>

                        <div className="flex items-center gap-4">
                            <select
                                value={selectedTag}
                                onChange={(e) => setSelectedTag(e.target.value)}
                                className="px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-[#1C387F] focus:border-transparent outline-none transition-all"
                            >
                                <option value="">All Tags</option>
                                {allTags.map((tag) => (
                                    <option key={tag} value={tag}>
                                        {tag}
                                    </option>
                                ))}
                            </select>
                            <div className="text-sm text-gray-500">
                                {filteredPosts.length} post{filteredPosts.length !== 1 ? "s" : ""}
                            </div>
                        </div>
                    </div>

                    {filteredPosts.length === 0 ? (
                        <div className="text-center py-12">
                            <div
                                className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                <Search className="w-8 h-8 text-gray-400"/>
                            </div>
                            <h3 className="text-lg font-semibold text-gray-900 mb-2">No posts found</h3>
                            <p className="text-gray-600">Try adjusting your search or filter criteria</p>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                            {filteredPosts.map((post) => (
                                <article
                                    key={post.id}
                                    className="bg-white rounded-lg shadow-sm border border-gray-100 hover:shadow-md transition-all duration-300 overflow-hidden group"
                                >
                                    <div className="p-6">
                                        <Link to={`/blog/${post.id}`} className="block">
                                            <h3 className="text-xl font-semibold text-gray-900 mb-3 group-hover:text-[#1C387F] transition-colors line-clamp-2">
                                                {post.title}
                                            </h3>
                                        </Link>

                                        <div className="flex items-center gap-4 text-sm text-gray-500 mb-4">
                                            <div className="flex items-center gap-1">
                                                <User className="w-4 h-4"/>
                                                <span>{post.author}</span>
                                            </div>
                                            <div className="flex items-center gap-1">
                                                <Calendar className="w-4 h-4"/>
                                                <span>{new Date(post.publishDate).toLocaleDateString()}</span>
                                            </div>
                                        </div>

                                        {post.tags && post.tags.length > 0 && (
                                            <div className="flex flex-wrap gap-2 mb-6">
                                                {post.tags.slice(0, 3).map((tag) => (
                                                    <span
                                                        key={tag}
                                                        className="inline-flex items-center gap-1 px-3 py-1 bg-[#EEF1F8] text-[#1C387F] text-xs font-medium rounded-full"
                                                    >
                            <Tag className="w-3 h-3"/>
                                                        {tag}
                          </span>
                                                ))}
                                                {post.tags.length > 3 && (
                                                    <span
                                                        className="px-3 py-1 bg-gray-100 text-gray-600 text-xs font-medium rounded-full">
                            +{post.tags.length - 3} more
                          </span>
                                                )}
                                            </div>
                                        )}

                                        <Link
                                            to={`/blog/${post.id}`}
                                            className="inline-flex items-center gap-2 px-4 py-2 bg-[#7f1c1c] hover:bg-[#5f1515] text-white font-medium rounded-lg transition-colors group"
                                        >
                                            Read More
                                            <ArrowRight
                                                className="w-4 h-4 group-hover:translate-x-1 transition-transform"/>
                                        </Link>
                                    </div>
                                </article>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </>
    )
}