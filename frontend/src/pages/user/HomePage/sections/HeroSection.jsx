import { Link } from 'react-router-dom';

export default function HeroSection() {
    return (
        <div className="bg-[#121212] h-[525px] flex">
            <div className="max-w-7xl mx-auto px-6 lg:px-10 flex flex-col-reverse md:flex-row items-center justify-center gap-10">
                <div className="w-full md:w-1/2">
                    <img
                        src="/pc-stack.png"
                        alt="PC setup"
                        className="w-full max-w-md mx-auto drop-shadow-[0_0_25px_#FF6B35]"
                    />
                </div>

                <div className="w-full md:w-1/2 text-center md:text-left">
                    <h1 className="text-3xl md:text-4xl font-bold text-[#FF6B35] mb-4">
                        POWER UP YOUR SETUP — PERFORMANCE BUILT FOR EVERY BATTLE
                    </h1>

                    <p className="text-gray-200 text-lg mb-6 leading-relaxed">
                        Whether you're gaming, creating, or upgrading your PC setup,<br />
                        the right hardware unlocks your true potential.
                    </p>

                    <Link
                        to="/shop"
                        className="inline-flex items-center gap-2 px-6 py-3 rounded-full 
                                   bg-[#FF6B35] text-white text-sm font-semibold shadow-md 
                                   hover:bg-[#E85F2F] transition"
                    >
                        SHOP NOW
                    </Link>
                </div>
            </div>
        </div>
    );
}