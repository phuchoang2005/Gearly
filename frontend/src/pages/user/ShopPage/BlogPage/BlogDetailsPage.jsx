import {Link} from "react-router-dom"
import {useParams} from "react-router-dom"
import {useQuery} from "@tanstack/react-query"
import {fetchBlogDetails} from "@u_services/blogService.js"
import {Calendar, User, Tag, Clock, ArrowLeft, BookOpen, Share2} from "lucide-react"

export default function BlogDetailsPage() {
    const {id} = useParams()

    const {
        data: blogPost,
        isLoading,
        isError,
        error,
    } = useQuery({
        queryKey: ["blogDetails", id],
        queryFn: () => fetchBlogDetails(id),
        enabled: !!id,
        staleTime: 1000 * 60 * 5,
    })

    const handleShare = () => {
        if (navigator.share) {
            navigator.share({
                title: blogPost.title,
                url: window.location.href,
            })
        } else {
            navigator.clipboard.writeText(window.location.href)
        }
    }

    if (isLoading) {
        return (
            <>
                <div className="min-h-screen bg-gray-50">
                    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
                        <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-8 animate-pulse">
                            <div className="h-8 bg-gray-200 rounded w-3/4 mb-6"></div>
                            <div className="flex gap-6 mb-8">
                                <div className="h-4 bg-gray-200 rounded w-32"></div>
                                <div className="h-4 bg-gray-200 rounded w-40"></div>
                            </div>
                            <div className="flex gap-2 mb-8">
                                <div className="h-6 bg-gray-200 rounded-full w-20"></div>
                                <div className="h-6 bg-gray-200 rounded-full w-24"></div>
                                <div className="h-6 bg-gray-200 rounded-full w-16"></div>
                            </div>
                            <div className="space-y-4">
                                {[...Array(8)].map((_, i) => (
                                    <div key={i} className="h-4 bg-gray-200 rounded w-full"></div>
                                ))}
                            </div>
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
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">Error Loading Post</h3>
                        <p className="text-red-500 mb-4">{error.message}</p>
                        <Link
                            to="/blog"
                            className="inline-flex items-center gap-2 px-4 py-2 bg-[#1C387F] hover:bg-[#152A5F] text-white font-medium rounded-lg transition-colors"
                        >
                            <ArrowLeft className="w-4 h-4"/>
                            Back to Blog
                        </Link>
                    </div>
                </div>
            </>
        )
    }

    if (!blogPost) {
        return (
            <>
                <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                    <div className="text-center">
                        <div
                            className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                            <BookOpen className="w-8 h-8 text-gray-400"/>
                        </div>
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">Blog Post Not Found</h3>
                        <p className="text-gray-600 mb-4">The post you're looking for doesn't exist or has been
                            removed.</p>
                        <Link
                            to="/blog"
                            className="inline-flex items-center gap-2 px-4 py-2 bg-[#1C387F] hover:bg-[#152A5F] text-white font-medium rounded-lg transition-colors"
                        >
                            <ArrowLeft className="w-4 h-4"/>
                            Back to Blog
                        </Link>
                    </div>
                </div>
            </>
        )
    }

    return (
        <>
            <div className="min-h-screen bg-gray-50">
                <div className="max-w-5xl mx-auto py-12">
                    <Link
                        to="/blog"
                        className="inline-flex items-center gap-2 text-[#1C387F] hover:text-[#152A5F] font-medium mb-8 transition-colors"
                    >
                        <ArrowLeft className="w-4 h-4"/>
                        Back to Blog
                    </Link>

                    <article className="bg-white rounded-lg shadow-sm border border-gray-100 overflow-hidden">
                        <div className="p-8 lg:p-16">
                            <header className="mb-8">
                                <h1 className="text-3xl lg:text-4xl font-bold text-gray-900 mb-6 leading-tight">{blogPost.title}</h1>

                                <div className="flex flex-wrap items-center gap-6 text-gray-500 mb-6">
                                    <div className="flex items-center gap-2">
                                        <User className="w-5 h-5"/>
                                        <span className="font-medium">{blogPost.author}</span>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        <Calendar className="w-5 h-5"/>
                                        <span>
                      {new Date(blogPost.publishDate).toLocaleDateString("en-US", {
                          year: "numeric",
                          month: "long",
                          day: "numeric",
                      })}
                    </span>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        <Clock className="w-5 h-5"/>
                                        <span>{Math.ceil(blogPost.content?.length / 1000) || 5} min read</span>
                                    </div>
                                </div>

                                {blogPost.tags && blogPost.tags.length > 0 && (
                                    <div className="flex flex-wrap gap-2 mb-6">
                                        {blogPost.tags.map((tag) => (
                                            <span
                                                key={tag}
                                                className="inline-flex items-center gap-1 px-3 py-1 bg-[#EEF1F8] text-[#1C387F] text-sm font-medium rounded-full"
                                            >
                        <Tag className="w-3 h-3"/>
                                                {tag}
                      </span>
                                        ))}
                                    </div>
                                )}

                                <div className="flex items-center justify-between pt-6 border-t border-gray-100">
                                    <div className="text-sm text-gray-500">Share this post</div>
                                    <button
                                        onClick={handleShare}
                                        className="inline-flex items-center gap-2 px-4 py-2 text-gray-600 hover:text-[#1C387F] hover:bg-[#EEF1F8] rounded-lg transition-colors"
                                    >
                                        <Share2 className="w-4 h-4"/>
                                        Share
                                    </button>
                                </div>
                            </header>

                            <div className="prose prose-lg max-w-none">
                                <div
                                    className="text-gray-800 leading-relaxed whitespace-pre-wrap text-justify  ">{blogPost.content}</div>
                            </div>

                            {blogPost.bookId && (
                                <div className="mt-12 pt-8 border-t border-gray-100">
                                    <div className="bg-[#EEF1F8] rounded-lg p-6">
                                        <h3 className="text-lg font-semibold text-gray-900 mb-2 flex items-center gap-2">
                                            <BookOpen className="w-5 h-5 text-[#1C387F]"/>
                                            Related Book
                                        </h3>
                                        <p className="text-gray-600 mb-4">This post is related to one of our featured
                                            books.</p>
                                        <Link
                                            to={`/book/${blogPost.bookId}`}
                                            className="inline-flex items-center gap-2 px-4 py-2 bg-[#1C387F] hover:bg-[#152A5F] text-white font-medium rounded-lg transition-colors"
                                        >
                                            View Book Details
                                            <ArrowLeft className="w-4 h-4 rotate-180"/>
                                        </Link>
                                    </div>
                                </div>
                            )}
                        </div>
                    </article>
                </div>
            </div>
        </>
    )
}