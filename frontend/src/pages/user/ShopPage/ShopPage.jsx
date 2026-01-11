import {useEffect, useMemo, useRef, useState} from "react";
import {useSearchParams} from "react-router-dom";
import {useShopData} from "@u_hooks/useShopData";
import HeaderBreadcrumb from "@u_components/shared/HeaderBreadcrumb";
import SearchAndCount from "./sections/SearchAndCount";
import ActiveFilters from "./sections/ActiveFilters";
import MobileFiltersToggle from "./sections/MobileFiltersToggle";
import FiltersSidebar from "./sections/FiltersSidebar";
import BookGrid from "./sections/BookGrid";
import Pagination from "./sections/Pagination";
import LoadingScreen from "@u_components/shared/LoadingScreen";
import ErrorScreen from "@u_components/shared/ErrorScreen";
import { safeInteger, safeNumber, safeString } from "@utils/safeType"
import {useDebounce} from "use-debounce";

const validSortOptions = new Set(["title-az", "title-za", "newest", "price-low", "price-high"]);
const validConditionOptions = new Set(["NEW", "LIKE NEW", "GOOD", "ACCEPTABLE"]);

const safeGenres = (value) => {
    return value ? value.split(",")?.filter((genre) => genre.trim() !== "") : [];
};

export default function ShopPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const isFirst = useRef(true);
    const gridRef = useRef(null)
    const searchRef = useRef(null);
    const previousSearch = useRef("")

    const [state, setState] = useState(() => {
        const pageNumber = safeInteger(searchParams.get("page"), 1, Infinity, 1);

        // 🔴 CHỈ SỬA RANGE 1–2000
        const minPrice = safeNumber(searchParams.get("minPrice"), 1, 1999, 1);
        const maxPrice = safeNumber(searchParams.get("maxPrice"), 2, 2000, 2000);

        const minRating = safeNumber(searchParams.get("minRating"), 0, 5, 0);
        const sortBy = safeString(searchParams.get("sortBy"), validSortOptions, "title-az");
        const condition = safeString(searchParams.get("condition"), validConditionOptions, "");
        const selectedGenres = safeGenres(searchParams.get("genres"));
        const searchText = searchParams.get("search") || "";

        return {
            page: {
                number: pageNumber - 1,
                size: 12,
                totalPages: 0,
                totalElements: 0,
            },
            condition,
            priceRange: [minPrice, maxPrice],
            selectedGenres,
            sortBy,
            searchText,
            minRating,
            mobileFiltersOpen: false,
            showAllConditions: false,
            showAllRatings: false,
        };
    });

    const updateState = (key, value) => {
        setState((prev) => ({
            ...prev,
            [key]: value,
        }));
    };

    const updatePageNumber = (number) => {
        setState((prev) => ({
            ...prev,
            page: { ...prev.page, number }
        }));
    };

    const queryParams = useMemo(() => ({
        condition: state.condition,
        minPrice: state.priceRange[0],
        maxPrice: state.priceRange[1],
        genres: state.selectedGenres.join(","),
        sortBy: state.sortBy,
        search: state.searchText,
        minRating: state.minRating === 0 ? state.minRating : state.minRating - 0.25,
        page: state.page.number,
        size: state.page.size,
    }), [state]);

    const [debouncedParams] = useDebounce(queryParams, 400);
    const { categories, books, isLoading, isError } = useShopData(debouncedParams);

    useEffect(() => {
        if (searchRef.current) searchRef.current.focus();
        if (!books) return;
        const {number, size, totalPages, totalElements} = books;
        updateState("page", {
            number: totalPages !== 0 ? Math.min(Math.max(number, 0), totalPages - 1) : 0,
            size,
            totalPages,
            totalElements
        });
    }, [books]);

    useEffect(() => {
        const currentSearch = searchParams.get("search") || ""
        if (previousSearch.current !== currentSearch) {
            previousSearch.current = currentSearch
            setState(prev => ({
                ...prev,
                searchText: currentSearch,
                page: { ...prev.page, number: 0 }
            }))
        }
    }, [searchParams])

    useEffect(() => {
        if (isFirst.current) {
            isFirst.current = false;
            return;
        }

        const params = {};
        if (state.condition) params.condition = state.condition;
        if (state.priceRange[0] !== 1) params.minPrice = state.priceRange[0];
        if (state.priceRange[1] !== 2000) params.maxPrice = state.priceRange[1];
        if (state.selectedGenres.length) params.genres = state.selectedGenres.join(",");
        if (state.sortBy !== "title-az") params.sortBy = state.sortBy;
        if (state.searchText) params.search = state.searchText;
        if (state.minRating > 0) params.minRating = state.minRating;
        if (state.page.number) params.page = state.page.number + 1;

        setSearchParams(params, { replace: true });
    }, [
        state.condition,
        state.priceRange,
        state.selectedGenres,
        state.sortBy,
        state.searchText,
        state.minRating,
        state.page,
    ]);

    const mapGenreIdsToNames = useMemo(() => {
        if (!Array.isArray(state.selectedGenres) || !Array.isArray(categories)) return [];
        const categoryMap = new Map(categories.map((c) => [c.id, c.name]));
        return state.selectedGenres
            .map((catId) => categoryMap.get(catId))
            .filter(Boolean);
    },[categories, state.selectedGenres]);

    const activeFilters = useMemo(() => {
        const af = [];
        if (state.condition) af.push({ type: "condition", value: `Condition: ${state.condition}` });
        if (mapGenreIdsToNames.length > 0) af.push({ type: "selectedGenres", value: `Genres: ${mapGenreIdsToNames.join(", ")}` });
        if (state.priceRange[0] !== 1 || state.priceRange[1] !== 2000)
            af.push({ type: "priceRange", value: `Price: $${state.priceRange[0]} - $${state.priceRange[1]}` });
        if (state.searchText) af.push({ type: "searchText", value: `Search: ${state.searchText}` });
        if (state.minRating > 0) af.push({ type: "minRating", value: `Rating: ${state.minRating}+` });
        if (state.sortBy !== "title-az") af.push({ type: "sortBy", value: `Sort By: ${state.sortBy}` });
        return af;
    }, [state, mapGenreIdsToNames]);

    const genPageIndex = useMemo(() => {
        const {totalPages, number} = state.page;
        if (totalPages <= 5) return Array.from({length: totalPages}, (_, i) => i + 1);

        const pages = [];
        const current = number + 1;
        const start = Math.max(2, current - 1);
        const end = Math.min(totalPages - 1, current + 1);

        pages.push(1);
        if (start > 2) pages.push("...");

        for (let i = start; i <= end; i++) pages.push(i);

        if (end < totalPages - 1) pages.push("...");
        pages.push(totalPages);
        return pages;
    },[state.page]);

    const handleGenreChange = (g) => {
        updateState(
            "selectedGenres",
            state.selectedGenres.includes(g)
                ? state.selectedGenres.filter((x) => x !== g)
                : [...state.selectedGenres, g]
        );
    };

    const removeFilter = (type) => {
        const resetValues = {
            condition: "",
            selectedGenres: [],
            priceRange: [1, 2000],
            searchText: "",
            minRating: 0,
            sortBy: "title-az",
        };

        if (type in resetValues) updateState(type, resetValues[type]);
        updatePageNumber(0);
    };

    const clearAll = () => {
        setState((prev) => ({
            ...prev,
            condition: "",
            selectedGenres: [],
            priceRange: [1, 2000],
            sortBy: "title-az",
            searchText: "",
            minRating: 0,
            page: { ...prev.page, number: 0 },
        }));
    };

    return (
        <div className="w-full mb-10 bg-white text-black">
            <HeaderBreadcrumb
                title="PC Shop"
                crumbs={[{ name: "Home", path: "/" }, { name: "Shop", path: "/shop" }]}
            />

            <div className="max-w-screen-xl mx-auto px-4" ref={gridRef}>
                <SearchAndCount
                    total={state.page.totalElements}
                    currentPage={state.page.number + 1}
                    itemsPerPage={state.page.size}
                    onSearchChange={(text) => updateState("searchText", text)}
                    value={state.searchText}
                    searchRef={searchRef}
                />

                <ActiveFilters
                    activeFilters={activeFilters}
                    removeFilter={removeFilter}
                    clearAll={clearAll}
                />

                <MobileFiltersToggle
                    openCount={activeFilters.length}
                    mobileOpen={state.mobileFiltersOpen}
                    toggleMobile={() => updateState("mobileFiltersOpen", !state.mobileFiltersOpen)}
                />

                <div className="flex flex-col md:flex-row md:gap-6">
                    <FiltersSidebar
                        condition={state.condition}
                        setCondition={(v) => updateState("condition", v)}
                        showAllConditions={state.showAllConditions}
                        toggleShowAllConditions={() => updateState("showAllConditions", !state.showAllConditions)}
                        minRating={state.minRating}
                        setMinRating={(v) => updateState("minRating", v)}
                        showAllRatings={state.showAllRatings}
                        toggleShowAllRatings={() => updateState("showAllRatings", !state.showAllRatings)}
                        priceRange={state.priceRange}
                        setPriceRange={(v) => updateState("priceRange", v)}
                        genres={categories || []}
                        selectedGenres={state.selectedGenres}
                        handleGenreChange={handleGenreChange}
                        sortBy={state.sortBy}
                        setSortBy={(v) => updateState("sortBy", v)}
                        mobileFiltersOpen={state.mobileFiltersOpen}
                    />

                    <div className="flex-1 ml-10">
                        {isLoading ? (
                            <LoadingScreen />
                        ) : isError ? (
                            <ErrorScreen />
                        ) : (
                            <>
                                <BookGrid books={books.content} clearAllFilters={clearAll} />
                                <Pagination
                                    totalPages={state.page.totalPages}
                                    currentPage={state.page.number + 1}
                                    onPageChange={(page) => updatePageNumber(page - 1)}
                                    pageIndex={genPageIndex}
                                />
                            </>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
