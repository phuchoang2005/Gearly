import {useQuery} from "@tanstack/react-query"
import {fetchStaticPageBySlug} from "@u_services/pageService.js"
import {Calendar} from "lucide-react"

export default function AboutUsPage() {
    const {
        data: aboutUsContent,
        isLoading,
        isError,
        error,
    } = useQuery({
        queryKey: ["staticPage", "about-us"],
        queryFn: () => fetchStaticPageBySlug("about-us"),
        staleTime: 1000 * 60 * 60,
    })

    if (isLoading) {
        return (
            <div className="min-h-screen bg-gray-50">
                <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
                    <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-8 lg:p-12 animate-pulse">
                        <div className="text-center mb-12">
                            <div className="h-10 bg-gray-200 rounded w-2/3 mx-auto mb-4"></div>
                            <div className="h-4 bg-gray-200 rounded w-1/2 mx-auto"></div>
                        </div>
                        <div className="space-y-4">
                            {[...Array(8)].map((_, i) => (
                                <div key={i} className="h-4 bg-gray-200 rounded w-full"></div>
                            ))}
                        </div>
                        <div className="mt-8 pt-6 border-t border-gray-100">
                            <div className="h-4 bg-gray-200 rounded w-1/3 ml-auto"></div>
                        </div>
                    </div>
                </div>
            </div>
        )
    }

    if (isError) {
        return (
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
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">Error Loading Content</h3>
                    <p className="text-red-500">{error.message}</p>
                </div>
            </div>
        )
    }

    if (!aboutUsContent) {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                <div className="text-center">
                    <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                        <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                            />
                        </svg>
                    </div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">Content Not Found</h3>
                    <p className="text-gray-600">About Us content is not available at the moment.</p>
                </div>
            </div>
        )
    }

    return (
        <div className="min-h-screen bg-gray-50">
            <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
                <article className="bg-white rounded-lg shadow-sm border border-gray-100 overflow-hidden">
                    <div className="bg-gradient-to-r from-[#7f1c1c] to-[#5f1515] px-8 lg:px-12 py-12 text-center">
                        <h1 className="text-3xl lg:text-4xl font-bold text-white mb-4 leading-tight">{aboutUsContent.title}</h1>
                        <div className="w-24 h-1 bg-white/30 mx-auto"></div>
                    </div>

                    <div className="p-8 lg:p-12">
                        <div className="prose prose-lg max-w-none">
                            <div
                                className="text-gray-800 leading-relaxed whitespace-pre-wrap text-lg">{aboutUsContent.content}</div>
                        </div>

                        {aboutUsContent.lastModified && (
                            <div className="mt-12 pt-8 border-t border-gray-100">
                                <div className="flex items-center justify-end text-sm text-gray-500">
                                    <div className="flex items-center gap-2">
                                        <Calendar className="w-4 h-4"/>
                                        <span>
                      Last updated:{" "}
                                            {new Date(aboutUsContent.lastModified).toLocaleDateString("en-US", {
                                                year: "numeric",
                                                month: "long",
                                                day: "numeric",
                                                hour: "2-digit",
                                                minute: "2-digit",
                                            })}
                    </span>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </article>

                <div className="mt-8 grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6 text-center">
                        <div
                            className="w-12 h-12 bg-[#EEF1F8] rounded-full flex items-center justify-center mx-auto mb-4">
                            <svg className="w-6 h-6 text-[#1C387F]" fill="none" stroke="currentColor"
                                 viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                                      d="M13 10V3L4 14h7v7l9-11h-7z"/>
                            </svg>
                        </div>
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">Mission</h3>
                        <p className="text-gray-600 text-sm">To provide reliable, high-quality technology products with honest pricing and excellent customer service</p>
                    </div>

                    <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6 text-center">
                        <div
                            className="w-12 h-12 bg-[#EEF1F8] rounded-full flex items-center justify-center mx-auto mb-4">
                            <svg className="w-6 h-6 text-[#1C387F]" fill="none" stroke="currentColor"
                                 viewBox="0 0 24 24">
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={2}
                                    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                                />
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={2}
                                    d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
                                />
                            </svg>
                        </div>
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">Vision</h3>
                        <p className="text-gray-600 text-sm">To become a trusted technology partner for customers by making modern computing accessible, simple, and dependable</p>
                    </div>

                    <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6 text-center">
                        <div
                            className="w-12 h-12 bg-[#EEF1F8] rounded-full flex items-center justify-center mx-auto mb-4">
                            <svg className="w-6 h-6 text-[#1C387F]" fill="none" stroke="currentColor"
                                 viewBox="0 0 24 24">
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={2}
                                    d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
                                />
                            </svg>
                        </div>
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">Values</h3>
                        <p className="text-gray-600 text-sm">Customer trust, product quality, transparency, and continuous improvement are our priorities</p>
                    </div>
                </div>
            </div>
        </div>
    )
}
