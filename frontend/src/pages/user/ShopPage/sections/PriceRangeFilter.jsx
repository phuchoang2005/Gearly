import React, { useRef, useState, useEffect } from "react";

export default function PriceRangeFilter({ priceRange, setPriceRange }) {
    const [min, max] = priceRange;
    const trackRef = useRef(null);

    const [minInput, setMinInput] = useState(min.toString());
    const [maxInput, setMaxInput] = useState(max.toString());

    useEffect(() => {
        setMinInput(min.toString());
        setMaxInput(max.toString());
    }, [min, max]);

    const clamp = (v, lo, hi) => Math.min(Math.max(v, lo), hi);

    const handleMouseDown = (thumb) => (e) => {
        e.preventDefault();
        const rect = trackRef.current.getBoundingClientRect();

        const onMouseMove = (e) => {
            let pct = (e.clientX - rect.left) / rect.width;
            pct = Math.max(0, Math.min(1, pct));
            const value = Math.round(pct * 1999) + 1;

            if (thumb === "min" && value < max) setPriceRange([value, max]);
            if (thumb === "max" && value > min) setPriceRange([min, value]);
        };

        const onMouseUp = () => {
            document.removeEventListener("mousemove", onMouseMove);
            document.removeEventListener("mouseup", onMouseUp);
        };

        document.addEventListener("mousemove", onMouseMove);
        document.addEventListener("mouseup", onMouseUp);
    };

    const handleInputChange = (type) => (e) => {
        let value = e.target.value.replace(/[^0-9]/g, "");

        if (value !== "0") value = value.replace(/^0+/, "");
        if (value === "") {
            if (type === "min") setMinInput("");
            if (type === "max") setMaxInput("");
            return;
        }

        if (type === "min") setMinInput(value);
        if (type === "max") setMaxInput(value);
    };

    const handleBlur = (type) => () => {
        let value = type === "min" ? Number(minInput) : Number(maxInput);

        if (type === "min") {
            if (minInput === "" || isNaN(value)) value = min;
            value = clamp(value, 1, max - 1);
            setMinInput(value.toString());
            setPriceRange([value, max]);
        } else {
            if (maxInput === "" || isNaN(value)) value = max;
            value = clamp(value, min + 1, 2000);
            setMaxInput(value.toString());
            setPriceRange([min, value]);
        }
    };

    const handleKeyDown = (type) => (e) => {
        if (e.key === "Enter" || e.key === "Escape") {
            handleBlur(type)();
            e.target.blur();
        }
    };

    const handleBarClick = (e) => {
        const rect = trackRef.current.getBoundingClientRect();
        let pct = (e.clientX - rect.left) / rect.width;
        pct = Math.max(0, Math.min(1, pct));
        const value = Math.round(pct * 1999) + 1;

        const distanceToMin = Math.abs(value - min);
        const distanceToMax = Math.abs(value - max);

        if (distanceToMin < distanceToMax) {
            setPriceRange([clamp(value, 1, max - 1), max]);
        } else {
            setPriceRange([min, clamp(value, min + 1, 2000)]);
        }
    };

    return (
        <div className="mb-4">
            <h3 className="font-semibold mb-2 text-sm text-gray-800">
                Price Range
            </h3>

            <div className="px-1 pt-1 mb-3">
                <div
                    ref={trackRef}
                    onClick={handleBarClick}
                    className="relative h-1 bg-gray-300 rounded-full mb-4 mt-6 cursor-pointer"
                >
                    <div
                        className="absolute h-1 bg-[#D70018] rounded-full"
                        style={{
                            left: `${(min / 2000) * 100}%`,
                            right: `${100 - (max / 2000) * 100}%`,
                        }}
                    />
                    <div
                        onMouseDown={handleMouseDown("min")}
                        className="absolute w-4 h-4 bg-black border-2 border-[#D70018] rounded-full -mt-1.5 -ml-2 cursor-grab shadow-sm hover:scale-110 transition-transform"
                        style={{ left: `${(min / 2000) * 100}%` }}
                    />
                    <div
                        onMouseDown={handleMouseDown("max")}
                        className="absolute w-4 h-4 bg-black border-2 border-[#D70018] rounded-full -mt-1.5 -ml-2 cursor-grab shadow-sm hover:scale-110 transition-transform"
                        style={{ left: `${(max / 2000) * 100}%` }}
                    />
                </div>
            </div>

            <div className="flex items-center gap-2">
                <div className="relative flex-1">
                    <span className="absolute left-2 top-1.5 text-xs text-gray-500">$</span>
                    <input
                        type="text"
                        value={minInput}
                        onChange={handleInputChange("min")}
                        onBlur={handleBlur("min")}
                        onKeyDown={handleKeyDown("min")}
                        className="w-full p-1.5 pl-5 border rounded-md text-xs
                                   focus:ring-1 focus:ring-[#D70018] focus:border-[#D70018]"
                    />
                </div>

                <span className="text-xs text-gray-500">to</span>

                <div className="relative flex-1">
                    <span className="absolute left-2 top-1.5 text-xs text-gray-500">$</span>
                    <input
                        type="text"
                        value={maxInput}
                        onChange={handleInputChange("max")}
                        onBlur={handleBlur("max")}
                        onKeyDown={handleKeyDown("max")}
                        className="w-full p-1.5 pl-5 border rounded-md text-xs
                                   focus:ring-1 focus:ring-[#D70018] focus:border-[#D70018]"
                    />
                </div>
            </div>
        </div>
    );
}
