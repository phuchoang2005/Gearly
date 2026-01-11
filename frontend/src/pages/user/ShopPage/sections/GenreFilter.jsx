import React from 'react'

export default function GenreFilter({genres, selectedGenres, handleGenreChange}) {
    return (
        <div className="mb-4">
            <h3 className="font-semibold mb-2 text-sm">Genre</h3>
            <div className="max-h-39 overflow-y-auto pr-1 space-y-1">
                {genres.map(genre => (
                    <label key={genre.id} className="flex items-center space-x-2 cursor-pointer">
                        <input type="checkbox" checked={selectedGenres.includes(genre.id)}
                               onChange={() => handleGenreChange(genre.id)}
                               className="w-3.5 h-3.5 text-blue-600 rounded focus:ring-blue-500 accent-[#1C387F]"
                        />
                        <span className="text-xs">{genre.name}</span>
                        <span className="text-xs text-gray-500 ml-auto">{genre.bookCount|| 0}</span>
                    </label>
                ))}
            </div>
        </div>
    )
}