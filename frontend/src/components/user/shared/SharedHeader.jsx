import { Link } from "react-router-dom";
import NavCore from "@u_components/shared/NavCore.jsx";
import logoWhite from "@assets/brand-logo-white.png";

export default function SharedHeader() {
    return (
        <header className="shadow">
            <div className="bg-[#000000]">
                <div className="max-w-7xl mx-auto py-3 flex">
                    <nav className="ml-auto flex gap-20 text-sm font-medium text-white">
                        {[
                            { to: "/", label: "Home" },
                            { to: "/shop", label: "Shop" },
                            { to: "/about", label: "About Us" },
                        ].map(({ to, label }) => (
                            <Link key={label} to={to} className="relative group">
                                {label}
                                <span className="absolute left-0 -bottom-1 w-0 h-0.5 bg-white transition-all duration-300 group-hover:w-full"></span>
                            </Link>
                        ))}
                    </nav>
                </div>
            </div>

            <NavCore
                logoSrc={logoWhite}
                bgClass="bg-[#D70018]"
                iconColor="text-white"
                padding="py-4"
            />
        </header>
    );
}