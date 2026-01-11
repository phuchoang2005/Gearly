import { useState } from "react"

export function UserAvatar({ profileAvatar, name = "", size = "md", onClick, className = "", showRing = true }) {
    const [imageError, setImageError] = useState(false)

    const getInitial = () => {
        if (!name) return "U"
        return name.charAt(0).toUpperCase()
    }

    const sizeClasses = {
        sm: "w-8 h-8 text-xs",
        md: "w-10 h-10 text-sm",
        lg: "w-12 h-12 text-base",
        xl: "w-24 h-24 text-xl",
    }

    const ringClasses = showRing ? "shadow ring-2 ring-white" : ""

    return (
        <>
            {profileAvatar && !imageError ? (
                <img
                    src={`${profileAvatar}?${Date.now()}`}
                    alt={`${name || "User"}'s avatar`}
                    onError={() => setImageError(true)}
                    className={`${sizeClasses[size]} ${ringClasses} rounded-full object-cover cursor-pointer transition-all duration-150 ${className}`}
                    onClick={onClick}
                />
            ) : (
                <div
                    className={`${sizeClasses[size]} ${ringClasses} rounded-full bg-blue-100 flex items-center justify-center text-blue-700 font-bold cursor-pointer transition-all duration-150 ${className}`}
                    onClick={onClick}
                >
                    {name ? name : getInitial()}
                </div>
            )}
        </>
    )
}
