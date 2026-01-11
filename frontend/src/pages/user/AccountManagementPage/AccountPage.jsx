import { useMemo, useState, useEffect } from "react";
import { useLocation, useNavigate, Outlet } from "react-router-dom";
import { User, Shield, Package, ChevronRight } from "lucide-react";
import HeaderBreadcrumb from "@u_components/shared/HeaderBreadcrumb.jsx";
import { useOrderStats } from "@u_hooks/useOrderStats.js";

export default function AccountPage() {
    const location = useLocation();
    const navigate = useNavigate();
    const [isLoading, setIsLoading] = useState(false);

    const {
        data: statsData,
        isLoading: statsLoading,
        isError: statsError,
    } = useOrderStats();
    const inProgressCount = statsData?.totalInProgress ?? 0;

    const tabs = [
        {
            id: "personal",
            label: "Personal Information",
            path: "/me",
            icon: User,
            description: "Manage your profile and personal details",
            badge: null,
        },
        {
            id: "security",
            label: "Security",
            path: "/me/security",
            icon: Shield,
            description: "Password and security settings",
            badge: null,
        },
        {
            id: "orders",
            label: "My Orders",
            path: "/me/orders",
            icon: Package,
            description: "View your order history and tracking",
            badge: inProgressCount,
        },
    ];

    const getActiveTab = () => {
        const sub = location.pathname.replace(/^\/me\/?/, "");
        if (sub.startsWith("security")) return "security";
        if (sub.startsWith("orders")) return "orders";
        return "personal";
    };

    const [activeTab, setActiveTab] = useState(getActiveTab());

    useEffect(() => {
        setActiveTab(getActiveTab());
    }, [location.pathname]);

    const handleTabChange = (tab) => {
        if (activeTab === tab.id) return;

        setIsLoading(true);
        setActiveTab(tab.id);

        setTimeout(() => {
            navigate(tab.path);
            setIsLoading(false);
        }, 150);
    };

    const handleKeyDown = (event, tab) => {
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            handleTabChange(tab);
        }
    };

    const MemoHeader = useMemo(
        () => (
            <HeaderBreadcrumb
                title="My Account"
                crumbs={[
                    { name: "Home", path: "/" },
                    { name: "Account Information", path: `/me` },
                ]}
            />
        ),
        []
    );

    const activeTabData = tabs.find((tab) => tab.id === activeTab);

    return (
        <div className="min-h-screen bg-neutral-100">
            {MemoHeader}

            <div className="max-w-7xl mx-auto px-4 py-8">
                <div className="bg-white rounded-2xl shadow-lg border border-gray-200 overflow-hidden">
                    {/* Header */}
                    <div className="border-b border-gray-200 px-6 py-5">
                        <div className="flex items-center justify-between mb-4">
                            <div>
                                <h1 className="text-2xl font-bold text-black">
                                    Account Settings
                                </h1>
                                <p className="text-gray-600 text-sm mt-1">
                                    Manage your account preferences and settings
                                </p>
                            </div>

                            {activeTabData && (
                                <div className="hidden md:flex items-center text-sm text-gray-500">
                                    <span>Current:</span>
                                    <ChevronRight size={16} className="mx-2" />
                                    <span className="font-semibold text-[#D70018]">
                                        {activeTabData.label}
                                    </span>
                                </div>
                            )}
                        </div>

                        {/* Tabs */}
                        <nav className="flex space-x-4" role="tablist">
                            {tabs.map((tab) => {
                                const Icon = tab.icon;
                                const isActive = activeTab === tab.id;

                                return (
                                    <button
                                        key={tab.id}
                                        onClick={() => handleTabChange(tab)}
                                        onKeyDown={(e) => handleKeyDown(e, tab)}
                                        role="tab"
                                        aria-selected={isActive}
                                        className={`
                                            group relative flex items-center gap-3 px-6 py-4 rounded-xl text-sm font-medium
                                            transition-all duration-200
                                            ${isActive
                                                ? "bg-[#D70018] text-white shadow-md"
                                                : "text-gray-700 hover:bg-gray-100"}
                                        `}
                                    >
                                        <div className="relative">
                                            <Icon size={20} />
                                            {tab.badge > 0 && (
                                                <span className="absolute -top-2 -right-2 bg-black text-white text-[9px] rounded-full h-4 w-4 flex items-center justify-center font-bold">
                                                    {tab.badge}
                                                </span>
                                            )}
                                        </div>

                                        <div className="flex flex-col items-start">
                                            <span className="font-semibold">
                                                {tab.label}
                                            </span>
                                            <span
                                                className={`text-xs ${
                                                    isActive
                                                        ? "text-red-100"
                                                        : "text-gray-500"
                                                }`}
                                            >
                                                {tab.description}
                                            </span>
                                        </div>
                                    </button>
                                );
                            })}
                        </nav>
                    </div>

                    {/* Content */}
                    <div className="relative">
                        {isLoading && (
                            <div className="absolute inset-0 bg-white/80 backdrop-blur-sm z-10 flex items-center justify-center">
                                <div className="flex items-center gap-3">
                                    <div className="animate-spin rounded-full h-6 w-6 border-2 border-[#D70018] border-t-transparent"></div>
                                    <span className="text-gray-700 font-medium">
                                        Loading...
                                    </span>
                                </div>
                            </div>
                        )}

                        <div
                            className={`p-8 transition-opacity ${
                                isLoading ? "opacity-50" : "opacity-100"
                            }`}
                            role="tabpanel"
                        >
                            <Outlet context={{ statsData }} />
                        </div>
                    </div>
                </div>

                {/* Quick Actions */}
                <div className="mt-6 bg-white rounded-xl shadow-sm border border-gray-200 p-6">
                    <h3 className="text-lg font-semibold text-black mb-4">
                        Quick Actions
                    </h3>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        {tabs.map((tab) => {
                            const Icon = tab.icon;
                            return (
                                <button
                                    key={tab.id}
                                    onClick={() => handleTabChange(tab)}
                                    onKeyDown={(e) => handleKeyDown(e, tab)}
                                    className="flex items-center gap-3 p-4 rounded-lg border border-gray-200 hover:border-[#D70018] hover:bg-[#D70018]/5 transition"
                                >
                                    <div className="p-2 bg-gray-100 rounded-lg">
                                        <Icon size={20} />
                                    </div>
                                    <div className="text-left">
                                        <p className="font-medium text-black">
                                            {tab.label}
                                        </p>
                                        <p className="text-sm text-gray-500">
                                            {tab.description}
                                        </p>
                                    </div>
                                </button>
                            );
                        })}
                    </div>
                </div>
            </div>
        </div>
    );
}
