import {Filter} from "lucide-react"

export function OrderFilters({statuses, selectedStatus, onStatusChange}) {
    return (
        <div className="bg-white border border-gray-200 rounded-lg p-4">
            <div className="flex items-center mb-3">
                <Filter className="text-gray-600 mr-2" size={16}/>
                <span className="text-sm font-medium text-gray-700">Filter by Status</span>
            </div>

            <div className="flex flex-wrap gap-2">
                {statuses.map((status) => (
                    <button
                        key={status.value}
                        onClick={() => onStatusChange(status.value)}
                        className={`px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 ${
                            selectedStatus === status.value
                                ? "bg-[#1C387F] text-white shadow-md"
                                : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                        }`}
                    >
                        {status.label}
                        {status.count > 0 && (
                            <span
                                className={`ml-2 px-2 py-0.5 rounded-full text-xs ${
                                    selectedStatus === status.value ? "bg-white/20 text-white" : "bg-gray-300 text-gray-600"
                                }`}
                            >
                {status.count}
              </span>
                        )}
                    </button>
                ))}
            </div>
        </div>
    )
}
