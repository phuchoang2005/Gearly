import Pagination from "@u_pages/ShopPage/sections/Pagination.jsx";

export default function PrecomputePagination({ totalPages, currentPage, setState, scrollRef }) {
    const handlePageChange = (page) => {
        scrollRef?.current?.scrollIntoView({ behavior: "smooth" });
        setTimeout(() => {
            setState(prev => ({ ...prev, pageIndex: page - 1 }));
        }, 0);
    };

    const handlePrev = () => {
        scrollRef?.current?.scrollIntoView({ behavior: "smooth" });
        setTimeout(() => {
            setState(prev => ({ ...prev, pageIndex: Math.max(prev.pageIndex - 1, 0) }));
        }, 0);
    };

    const handleNext = () => {
        scrollRef?.current?.scrollIntoView({ behavior: "smooth" });
        setTimeout(() => {
            setState(prev => ({ ...prev, pageIndex: Math.min(prev.pageIndex + 1, totalPages - 1) }));
        }, 0);
    };

    const genPageIndex = () => {
        if (totalPages <= 5) return Array.from({ length: totalPages }, (_, i) => i + 1);

        const pages = [];
        const current = currentPage;
        pages.push(1);
        const start = Math.max(2, current - 1);
        const end = Math.min(totalPages - 1, current + 1);

        if (start > 2) pages.push("...");
        for (let i = start; i <= end; i++) pages.push(i);
        if (end < totalPages - 1) pages.push("...");
        pages.push(totalPages);

        return pages;
    };

    return (
        <Pagination
            totalPages={totalPages}
            currentPage={currentPage}
            onPageChange={handlePageChange}
            onPrev={handlePrev}
            onNext={handleNext}
            pageIndex={genPageIndex()}
        />
    );
}
