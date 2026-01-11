import React from 'react'
import RatingFilter from './RatingFilter'
import PriceRangeFilter from './PriceRangeFilter'
import GenreFilter from './GenreFilter'
import SortBySelect from './SortBySelect'

export default function FiltersSidebar(props) {
    const {
        minRating, setMinRating, showAllRatings, toggleShowAllRatings,
        priceRange, setPriceRange,
        genres, selectedGenres, handleGenreChange,
        sortBy, setSortBy,
        mobileFiltersOpen
    } = props

    return (
        <div
            className={`w-full md:w-58 shrink-0 rounded-lg p-8 h-fit
            bg-[#F4F4F4] border-2 border-gray-400 drop-shadow-lg
            md:block md:top-16
            ${mobileFiltersOpen ? 'block' : 'hidden'}`}
        >
            {/* Rating */}
            <RatingFilter
                minRating={minRating}
                setMinRating={setMinRating}
                showAll={showAllRatings}
                toggleShowAll={toggleShowAllRatings}
            />

            <div className="h-px bg-gray-300 my-4"></div>

            {/* Price */}
            <PriceRangeFilter
                priceRange={priceRange}
                setPriceRange={setPriceRange}
            />

            <div className="h-px bg-gray-300 my-4"></div>

            {/* Category / PC Type */}
            <GenreFilter
                genres={genres}
                selectedGenres={selectedGenres}
                handleGenreChange={handleGenreChange}
            />

            <div className="h-px bg-gray-300 my-4"></div>

            {/* Sort */}
            <SortBySelect
                sortBy={sortBy}
                setSortBy={setSortBy}
            />
        </div>
    )
}
