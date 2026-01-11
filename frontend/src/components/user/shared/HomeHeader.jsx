import { Link } from "react-router-dom";
import AnnouncementBar from "@u_components/shared/AnnouncementBar.jsx";
import NavCore from "@u_components/shared/NavCore.jsx";

export default function HomeHeader() {
    return (
        <header className="shadow">
            <AnnouncementBar />

            <NavCore
                bgClass="bg-white"
                textColor="text-black"
                iconColor="text-black"
                padding="py-4"
            />

            <div className="bg-[#D70018] drop-shadow-lg z-10">
                <div className="max-w-7xl mx-auto py-4 flex justify-center gap-56 text-sm font-medium text-white">
                    {["Home", "Shop", "Blog", "About Us"].map((label) => (
                        <Link
                            key={label}
                            to={`/${label.toLowerCase().replace(" ", "-")}`}
                            className="relative group"
                        >
                            {label}
                            <span className="absolute left-0 -bottom-1 w-0 h-0.5 bg-white transition-all duration-300 group-hover:w-full"></span>
                        </Link>
                    ))}
                </div>
            </div>
        </header>
    );
}
